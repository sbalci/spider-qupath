# spider_qupath_classifier.py
# Working example of a Python script to classify annotations in QuPath using the SPIDER model.
import os
import sys
import json
import torch
import numpy as np
from PIL import Image
import openslide
from transformers import AutoModel, AutoProcessor
from pathlib import Path

# Parse command line arguments
if len(sys.argv) < 4:
    print("Usage: python spider_qupath_classifier.py <annotations_json> <model_path> <output_dir>")
    sys.exit(1)

annotations_path = sys.argv[1]
model_path = sys.argv[2]
output_dir = sys.argv[3]

# Create output directory if it doesn't exist
os.makedirs(output_dir, exist_ok=True)

# Configure device
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
print(f"Using device: {device}")

# Load SPIDER model
def load_spider_model(model_path):
    print(f"Loading SPIDER model from: {model_path}")
    
    try:
        # Load model and processor
        model = AutoModel.from_pretrained(
            model_path,
            trust_remote_code=True,
            local_files_only=True
        )
        
        processor = AutoProcessor.from_pretrained(
            model_path,
            trust_remote_code=True,
            local_files_only=True
        )
        
        # Move model to device
        model.to(device)
        model.eval()
        
        # Load class names from config
        config_path = os.path.join(model_path, "config.json")
        with open(config_path, 'r') as f:
            config = json.load(f)
        
        class_names = config.get('class_names', [])
        print(f"Model has {len(class_names)} classes: {class_names}")
        
        return model, processor, class_names
    
    except Exception as e:
        print(f"Error loading model: {str(e)}")
        sys.exit(1)

# Parse QuPath path format
def parse_qupath_path(qupath_path):
    # Handle BioFormatsImageServer paths
    if qupath_path.startswith("BioFormatsImageServer:"):
        path = qupath_path.replace("BioFormatsImageServer:", "").strip()
        if path.startswith("file:/"):
            path = path[6:]
        if "[" in path:
            path = path.split("[")[0]
        return path
    
    # Handle other path formats
    elif qupath_path.startswith("file:"):
        return qupath_path[5:]
    
    # Handle OpenslideImageServer paths
    elif "OpenslideImageServer:" in qupath_path:
        path = qupath_path.replace("qupath.lib.images.servers.openslide.OpenslideImageServer:", "").strip()
        if path.startswith("file:/"):
            path = path[6:]
        return path
        
    return qupath_path

# Extract region from slide with context padding
def extract_region_with_context(slide_path, region):
    try:
        # Parse path
        parsed_path = parse_qupath_path(slide_path)
        print(f"Opening slide: {parsed_path}")
        
        # Open slide
        slide = openslide.OpenSlide(parsed_path)
        
        # Extract region coordinates
        x = int(region['x'])
        y = int(region['y'])
        width = int(region['width'])
        height = int(region['height'])
        
        # SPIDER uses 1120×1120 regions, so add context padding if necessary
        context_size = 1120
        
        # Calculate center of region
        center_x = x + width // 2
        center_y = y + height // 2
        
        # Calculate coordinates for context region
        context_x = max(0, center_x - context_size // 2)
        context_y = max(0, center_y - context_size // 2)
        
        # Make sure we don't go outside slide boundaries
        slide_width, slide_height = slide.dimensions
        if context_x + context_size > slide_width:
            context_x = max(0, slide_width - context_size)
        if context_y + context_size > slide_height:
            context_y = max(0, slide_height - context_size)
        
        # Extract the region with context
        region_img = slide.read_region((context_x, context_y), 0, (context_size, context_size)).convert('RGB')
        
        print(f"Extracted region with context at ({context_x}, {context_y}), size {context_size}x{context_size}")
        return region_img
    
    except Exception as e:
        print(f"Error extracting region: {str(e)}")
        return None

# Main classification function
def classify_annotations():
    # Load model
    model, processor, class_names = load_spider_model(model_path)
    
    # Save class names to output directory
    with open(os.path.join(output_dir, 'classes.json'), 'w') as f:
        json.dump(class_names, f)
    
    # Load annotations
    with open(annotations_path, 'r') as f:
        annotations = json.load(f)
    
    print(f"Loaded {len(annotations)} annotations for classification")
    
    # Initialize results
    results = []
    
    for idx, annotation in enumerate(annotations):
        print(f"Processing annotation {idx+1}/{len(annotations)}...")
        
        # Extract information
        slide_path = annotation['slide_path']
        region = annotation['roi']
        annotation_id = annotation['id']
        
        # Extract region with context
        region_img = extract_region_with_context(slide_path, region)
        
        if region_img is None:
            print(f"Could not extract region for annotation {annotation_id}")
            results.append({
                'id': annotation_id,
                'prediction': None,
                'probabilities': None
            })
            continue
        
        # Process with SPIDER model
        try:
            # Prepare inputs
            inputs = processor(images=region_img, return_tensors="pt")
            
            # Move inputs to device
            for k, v in inputs.items():
                if isinstance(v, torch.Tensor):
                    inputs[k] = v.to(device)
            
            # Run inference
            with torch.no_grad():
                outputs = model(**inputs)
            
            # Get predicted class and probabilities
            logits = outputs.logits
            probabilities = torch.softmax(logits[0], dim=0).cpu().numpy()
            prediction_idx = probabilities.argmax().item()
            prediction = class_names[prediction_idx]
            
            # Create class probabilities dictionary
            class_probabilities = {class_name: float(probabilities[i]) for i, class_name in enumerate(class_names)}
            
            print(f"Prediction for annotation {annotation_id}: {prediction}")
            
            # Store result
            result = {
                'id': annotation_id,
                'prediction': prediction,
                'probabilities': class_probabilities
            }
            
            results.append(result)
            
        except Exception as e:
            print(f"Error classifying annotation {annotation_id}: {str(e)}")
            results.append({
                'id': annotation_id,
                'prediction': None,
                'probabilities': None
            })
    
    # Save results
    results_path = os.path.join(output_dir, 'predictions.json')
    with open(results_path, 'w') as f:
        json.dump(results, f)
    
    print(f"Classified {len(results)} annotations")
    print(f"Saved predictions to {results_path}")
    return results

# Run classification
classify_annotations()