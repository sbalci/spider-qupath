# spider_qupath_classifier_universal.py
# Universal Python script to classify annotations in QuPath using any SPIDER model
# Works with Colorectal, Skin, and Thorax models

import os
import sys
import json
import torch
import numpy as np
from PIL import Image
import openslide
from transformers import AutoModel, AutoProcessor
from pathlib import Path
from datetime import datetime

# Parse command line arguments
if len(sys.argv) < 4:
    print("Usage: python spider_qupath_classifier_universal.py <annotations_json> <model_path> <output_dir>")
    sys.exit(1)

annotations_path = sys.argv[1]
model_path = sys.argv[2]
output_dir = sys.argv[3]

# Create output directory if it doesn't exist
os.makedirs(output_dir, exist_ok=True)

# Configure device
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
print(f"Using device: {device}")

# Model-specific color schemes for better visualization
MODEL_COLOR_SCHEMES = {
    "colorectal": {
        "Adenocarcinoma high grade": "#DC143C",  # Crimson
        "Adenocarcinoma low grade": "#FF4500",   # OrangeRed
        "Adenoma high grade": "#FF8C00",         # DarkOrange
        "Adenoma low grade": "#FFA500",          # Orange
        "Fat": "#FFD700",                        # Gold
        "Hyperplastic polyp": "#008000",         # Green
        "Inflammation": "#FFFF00",               # Yellow
        "Mucus": "#00BFFF",                      # DeepSkyBlue
        "Muscle": "#8B008B",                     # DarkMagenta
        "Necrosis": "#800000",                   # Maroon
        "Sessile serrated lesion": "#00FA9A",    # MediumSpringGreen
        "Stroma healthy": "#32CD32",             # LimeGreen
        "Vessels": "#0000FF"                     # Blue
    },
    "skin": {
        "Basal Cell Carcinoma": "#8B0000",       # DarkRed
        "Melanoma invasive": "#000000",          # Black
        "Melanoma in situ": "#696969",           # DimGray
        "Squamous Cell Carcinoma": "#DC143C",    # Crimson
        "Epidermis": "#FFE4B5",                  # Moccasin
        "Dermis": "#DEB887",                     # BurlyWood
        "Vessels": "#0000FF",                    # Blue
        # Add more skin classes as they appear in the model
    },
    "thorax": {
        "Small cell carcinoma": "#800080",       # Purple
        "Non-small cell carcinoma": "#4B0082",   # Indigo
        "Alveoli": "#87CEEB",                    # SkyBlue
        "Vessels": "#0000FF",                    # Blue
        "Bronchus": "#8B4513",                   # SaddleBrown
        # Add more thorax classes as they appear in the model
    }
}

# Load SPIDER model with automatic model type detection
def load_spider_model(model_path):
    print(f"Loading SPIDER model from: {model_path}")
    
    # Detect model type from path
    model_type = "unknown"
    if "colorectal" in model_path.lower():
        model_type = "colorectal"
    elif "skin" in model_path.lower():
        model_type = "skin"
    elif "thorax" in model_path.lower():
        model_type = "thorax"
    
    print(f"Detected model type: {model_type}")
    
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
        
        # Get color scheme for this model type
        color_scheme = MODEL_COLOR_SCHEMES.get(model_type, {})
        
        return model, processor, class_names, model_type, color_scheme
    
    except Exception as e:
        print(f"Error loading model: {str(e)}")
        sys.exit(1)

# Parse QuPath path format (handles various QuPath server types)
def parse_qupath_path(qupath_path):
    # Handle BioFormatsImageServer paths
    if qupath_path.startswith("BioFormatsImageServer:"):
        path = qupath_path.replace("BioFormatsImageServer:", "").strip()
        if path.startswith("file:/"):
            path = path[6:]
        if "[" in path:
            path = path.split("[")[0]
        return path
    
    # Handle file:// URLs
    elif qupath_path.startswith("file:///"):
        return qupath_path[8:]  # Remove file:///
    elif qupath_path.startswith("file://"):
        return qupath_path[7:]  # Remove file://
    elif qupath_path.startswith("file:/"):
        return qupath_path[6:]  # Remove file:/
    
    # Handle OpenslideImageServer paths
    elif "OpenslideImageServer:" in qupath_path:
        path = qupath_path.replace("qupath.lib.images.servers.openslide.OpenslideImageServer:", "").strip()
        if path.startswith("file:/"):
            path = path[6:]
        return path
    
    # Handle ImageIOImageServer paths
    elif "ImageIOImageServer:" in qupath_path:
        path = qupath_path.replace("qupath.lib.images.servers.imageio.ImageIOImageServer:", "").strip()
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
    model, processor, class_names, model_type, color_scheme = load_spider_model(model_path)
    
    # Save model information to output directory
    model_info = {
        'model_type': model_type,
        'class_names': class_names,
        'color_scheme': color_scheme,
        'timestamp': datetime.now().isoformat()
    }
    
    with open(os.path.join(output_dir, 'model_info.json'), 'w') as f:
        json.dump(model_info, f, indent=2)
    
    # Save class names separately for backward compatibility
    with open(os.path.join(output_dir, 'classes.json'), 'w') as f:
        json.dump(class_names, f)
    
    # Load annotations
    with open(annotations_path, 'r') as f:
        annotations = json.load(f)
    
    print(f"Loaded {len(annotations)} annotations for classification")
    
    # Initialize results
    results = []
    
    # History file for tracking all predictions
    history_file = os.path.join(output_dir, f'prediction_history_{model_type}.jsonl')
    
    for idx, annotation in enumerate(annotations):
        print(f"\nProcessing annotation {idx+1}/{len(annotations)}...")
        
        # Extract information
        slide_path = annotation['slide_path']
        region = annotation['roi']
        annotation_id = annotation['id']
        image_name = annotation.get('image_name', 'unknown')
        
        # Extract region with context
        region_img = extract_region_with_context(slide_path, region)
        
        if region_img is None:
            print(f"Could not extract region for annotation {annotation_id}")
            results.append({
                'id': annotation_id,
                'prediction': None,
                'probabilities': None,
                'top_predictions': None
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
            
            # Create class probabilities dictionary (rounded to 3 decimal places)
            class_probabilities = {class_name: round(float(probabilities[i]), 3) 
                                 for i, class_name in enumerate(class_names)}
            
            # Get top 3 predictions for display
            sorted_indices = np.argsort(probabilities)[::-1]
            top_predictions = []
            for i in range(min(3, len(class_names))):
                idx = sorted_indices[i]
                if probabilities[idx] > 0.01:  # Only include if probability > 1%
                    top_predictions.append({
                        'class': class_names[idx],
                        'probability': round(float(probabilities[idx]), 3),
                        'color': color_scheme.get(class_names[idx], '#808080')
                    })
            
            # Display results
            print(f"Prediction: {prediction} ({probabilities[prediction_idx]:.1%})")
            if len(top_predictions) > 1:
                print("Alternative predictions:")
                for i, pred in enumerate(top_predictions[1:], 1):
                    print(f"  {i}. {pred['class']}: {pred['probability']:.1%}")
            
            # Store result
            result = {
                'id': annotation_id,
                'prediction': prediction,
                'probabilities': class_probabilities,
                'top_predictions': top_predictions,
                'timestamp': datetime.now().isoformat(),
                'image_name': image_name,
                'model_type': model_type,
                'confidence': float(probabilities[prediction_idx])
            }
            
            # Append to history file
            with open(history_file, 'a') as f:
                f.write(json.dumps(result) + '\n')
            
            results.append(result)
            
        except Exception as e:
            print(f"Error classifying annotation {annotation_id}: {str(e)}")
            results.append({
                'id': annotation_id,
                'prediction': None,
                'probabilities': None,
                'top_predictions': None
            })
    
    # Save results
    results_path = os.path.join(output_dir, 'predictions.json')
    with open(results_path, 'w') as f:
        json.dump(results, f, indent=2)
    
    # Create summary statistics
    summary = {
        'total_annotations': len(results),
        'successful_classifications': sum(1 for r in results if r['prediction'] is not None),
        'model_type': model_type,
        'timestamp': datetime.now().isoformat()
    }
    
    if summary['successful_classifications'] > 0:
        # Count predictions by class
        class_counts = {}
        for result in results:
            if result['prediction']:
                class_counts[result['prediction']] = class_counts.get(result['prediction'], 0) + 1
        
        summary['class_distribution'] = class_counts
        
        # Calculate average confidence
        confidences = [r['confidence'] for r in results if r.get('confidence')]
        if confidences:
            summary['average_confidence'] = round(sum(confidences) / len(confidences), 3)
    
    with open(os.path.join(output_dir, 'classification_summary.json'), 'w') as f:
        json.dump(summary, f, indent=2)
    
    print(f"\nClassification completed!")
    print(f"Successfully classified {summary['successful_classifications']}/{len(results)} annotations")
    print(f"Results saved to {results_path}")
    print(f"Summary saved to classification_summary.json")
    
    return results

# Run classification
if __name__ == "__main__":
    classify_annotations()