# whole_slide_analysis_spider_universal.py
# Universal whole slide analysis script for all SPIDER models
# Generates heatmaps and visualizations for Colorectal, Skin, and Thorax models

import os
import sys
import json
import torch
import numpy as np
from PIL import Image
import openslide
from transformers import AutoModel, AutoProcessor
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.colors import LinearSegmentedColormap
from datetime import datetime
from pathlib import Path
import multiprocessing as mp
from functools import partial
import warnings
warnings.filterwarnings('ignore')

# Parse command line arguments
if len(sys.argv) < 4:
    print("Usage: python whole_slide_analysis_spider_universal.py <model_path> <svs_path> <output_folder> [patch_stride] [max_patches] [num_workers]")
    print("Example: python whole_slide_analysis_spider_universal.py ./SPIDER-skin-model ./slide.svs ./output 560 1000 4")
    sys.exit(1)

model_path = sys.argv[1]
svs_path = sys.argv[2]
output_folder = sys.argv[3]
patch_stride = int(sys.argv[4]) if len(sys.argv) > 4 else 560  # 50% overlap by default
max_patches = int(sys.argv[5]) if len(sys.argv) > 5 else 1000
num_workers = int(sys.argv[6]) if len(sys.argv) > 6 else 4

# Create output directory
os.makedirs(output_folder, exist_ok=True)

# Model-specific visualization settings
MODEL_SETTINGS = {
    "colorectal": {
        "name": "SPIDER Colorectal Analysis",
        "colors": {
            "Adenocarcinoma high grade": "#DC143C",
            "Adenocarcinoma low grade": "#FF4500",
            "Adenoma high grade": "#FF8C00",
            "Adenoma low grade": "#FFA500",
            "Fat": "#FFD700",
            "Hyperplastic polyp": "#008000",
            "Inflammation": "#FFFF00",
            "Mucus": "#00BFFF",
            "Muscle": "#8B008B",
            "Necrosis": "#800000",
            "Sessile serrated lesion": "#00FA9A",
            "Stroma healthy": "#32CD32",
            "Vessels": "#0000FF"
        }
    },
    "skin": {
        "name": "SPIDER Skin Analysis",
        "colors": {
            "Basal Cell Carcinoma": "#8B0000",
            "Melanoma invasive": "#000000",
            "Melanoma in situ": "#696969",
            "Squamous Cell Carcinoma": "#DC143C",
            "Epidermis": "#FFE4B5",
            "Dermis": "#DEB887",
            "Vessels": "#0000FF",
            "Hair follicle": "#8B4513",
            "Sebaceous gland": "#F0E68C",
            "Sweat gland": "#87CEEB"
        }
    },
    "thorax": {
        "name": "SPIDER Thorax Analysis",
        "colors": {
            "Small cell carcinoma": "#800080",
            "Non-small cell carcinoma": "#4B0082",
            "Alveoli": "#87CEEB",
            "Vessels": "#0000FF",
            "Bronchus": "#8B4513",
            "Pleura": "#F0E68C",
            "Lymphoid tissue": "#9370DB"
        }
    }
}

# Detect model type from path
def detect_model_type(model_path):
    model_path_lower = model_path.lower()
    if "colorectal" in model_path_lower:
        return "colorectal"
    elif "skin" in model_path_lower:
        return "skin"
    elif "thorax" in model_path_lower:
        return "thorax"
    else:
        print("Warning: Could not detect model type from path. Using default settings.")
        return "colorectal"

# Load SPIDER model
def load_spider_model(model_path):
    print(f"Loading SPIDER model from: {model_path}")
    
    model_type = detect_model_type(model_path)
    print(f"Detected model type: {model_type}")
    
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Using device: {device}")
    
    try:
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
        
        model.to(device)
        model.eval()
        
        # Load class names
        config_path = os.path.join(model_path, "config.json")
        with open(config_path, 'r') as f:
            config = json.load(f)
        
        class_names = config.get('class_names', [])
        print(f"Model has {len(class_names)} classes")
        
        return model, processor, class_names, device, model_type
    
    except Exception as e:
        print(f"Error loading model: {str(e)}")
        sys.exit(1)

# Process a single patch
def process_patch(patch_info, model_path):
    x, y, patch_size = patch_info
    
    # Reload model for each worker (required for multiprocessing)
    model, processor, class_names, device, _ = load_spider_model(model_path)
    
    try:
        # Extract patch
        patch = slide.read_region((x, y), 0, (patch_size, patch_size)).convert('RGB')
        
        # Process with model
        inputs = processor(images=patch, return_tensors="pt")
        for k, v in inputs.items():
            if isinstance(v, torch.Tensor):
                inputs[k] = v.to(device)
        
        with torch.no_grad():
            outputs = model(**inputs)
        
        logits = outputs.logits
        probabilities = torch.softmax(logits[0], dim=0).cpu().numpy()
        prediction_idx = probabilities.argmax().item()
        
        return {
            'x': x,
            'y': y,
            'prediction': class_names[prediction_idx],
            'probabilities': probabilities.tolist(),
            'confidence': float(probabilities[prediction_idx])
        }
    
    except Exception as e:
        print(f"Error processing patch at ({x}, {y}): {str(e)}")
        return None

# Main analysis
print(f"Starting whole slide analysis for: {svs_path}")

# Load slide
slide = openslide.OpenSlide(svs_path)
slide_width, slide_height = slide.dimensions
print(f"Slide dimensions: {slide_width} x {slide_height}")

# Load model for getting class names
model, processor, class_names, device, model_type = load_spider_model(model_path)

# Get model settings
settings = MODEL_SETTINGS.get(model_type, MODEL_SETTINGS["colorectal"])
color_map = settings["colors"]
analysis_name = settings["name"]

# Calculate patches to process
patch_size = 1120  # SPIDER input size
patches_to_process = []

# Simple grid sampling with stride
for y in range(0, slide_height - patch_size, patch_stride):
    for x in range(0, slide_width - patch_size, patch_stride):
        patches_to_process.append((x, y, patch_size))
        if len(patches_to_process) >= max_patches:
            break
    if len(patches_to_process) >= max_patches:
        break

print(f"Processing {len(patches_to_process)} patches with stride {patch_stride}")

# Process patches in parallel
if num_workers > 1:
    print(f"Using {num_workers} workers for parallel processing")
    # Close the slide object before multiprocessing
    slide.close()
    
    # Process patches
    with mp.Pool(num_workers) as pool:
        process_func = partial(process_patch, model_path=model_path)
        results = list(pool.imap_unordered(process_func, patches_to_process))
    
    # Reopen slide for thumbnail
    slide = openslide.OpenSlide(svs_path)
else:
    # Single-threaded processing
    results = []
    for i, patch_info in enumerate(patches_to_process):
        if i % 100 == 0:
            print(f"Processing patch {i+1}/{len(patches_to_process)}")
        result = process_patch(patch_info, model_path)
        results.append(result)

# Filter out failed patches
results = [r for r in results if r is not None]
print(f"Successfully processed {len(results)} patches")

# Save raw results
results_path = os.path.join(output_folder, 'patch_predictions.json')
with open(results_path, 'w') as f:
    json.dump(results, f)

# Create heatmaps for each class
print("Generating heatmaps...")

# Get thumbnail for overlay
thumbnail_size = (2000, int(2000 * slide_height / slide_width))
thumbnail = slide.get_thumbnail(thumbnail_size)

# Calculate scaling factors
scale_x = thumbnail_size[0] / slide_width
scale_y = thumbnail_size[1] / slide_height

# Create visualization for each class
fig, axes = plt.subplots(3, 4, figsize=(20, 15))
axes = axes.flatten()

for idx, class_name in enumerate(class_names[:12]):  # Show up to 12 classes
    ax = axes[idx]
    
    # Create heatmap for this class
    heatmap = np.zeros(thumbnail_size[::-1])  # height x width
    counts = np.zeros(thumbnail_size[::-1])
    
    for result in results:
        x_thumb = int(result['x'] * scale_x)
        y_thumb = int(result['y'] * scale_y)
        w_thumb = int(patch_size * scale_x)
        h_thumb = int(patch_size * scale_y)
        
        # Get probability for this class
        prob = result['probabilities'][idx]
        
        # Add to heatmap
        y_end = min(y_thumb + h_thumb, heatmap.shape[0])
        x_end = min(x_thumb + w_thumb, heatmap.shape[1])
        
        heatmap[y_thumb:y_end, x_thumb:x_end] += prob
        counts[y_thumb:y_end, x_thumb:x_end] += 1
    
    # Average the probabilities
    with np.errstate(divide='ignore', invalid='ignore'):
        heatmap = np.divide(heatmap, counts)
        heatmap[counts == 0] = 0
    
    # Show thumbnail with heatmap overlay
    ax.imshow(thumbnail, alpha=0.5)
    
    # Get color for this class
    class_color = color_map.get(class_name, '#808080')
    # Create custom colormap from white to class color
    colors = ['white', class_color]
    n_bins = 100
    cmap = LinearSegmentedColormap.from_list(class_name, colors, N=n_bins)
    
    im = ax.imshow(heatmap, alpha=0.7, cmap=cmap, vmin=0, vmax=1)
    ax.set_title(f"{class_name}", fontsize=12)
    ax.axis('off')

# Remove unused subplots
for idx in range(len(class_names), len(axes)):
    fig.delaxes(axes[idx])

plt.suptitle(f"{analysis_name} - {os.path.basename(svs_path)}", fontsize=16)
plt.tight_layout()
plt.savefig(os.path.join(output_folder, 'class_heatmaps.png'), dpi=150, bbox_inches='tight')
plt.close()

# Create overall classification map
print("Creating classification map...")

fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(20, 10))

# Show original thumbnail
ax1.imshow(thumbnail)
ax1.set_title("Original Slide", fontsize=14)
ax1.axis('off')

# Create classification overlay
classification_map = np.zeros((*thumbnail_size[::-1], 3))

for result in results:
    x_thumb = int(result['x'] * scale_x)
    y_thumb = int(result['y'] * scale_y)
    w_thumb = int(patch_size * scale_x)
    h_thumb = int(patch_size * scale_y)
    
    # Get color for predicted class
    predicted_class = result['prediction']
    color_hex = color_map.get(predicted_class, '#808080')
    color_rgb = [int(color_hex[i:i+2], 16)/255 for i in (1, 3, 5)]
    
    # Apply color with confidence-based opacity
    confidence = result['confidence']
    y_end = min(y_thumb + h_thumb, classification_map.shape[0])
    x_end = min(x_thumb + w_thumb, classification_map.shape[1])
    
    for c in range(3):
        classification_map[y_thumb:y_end, x_thumb:x_end, c] = color_rgb[c]

# Show classification map
ax2.imshow(thumbnail, alpha=0.3)
ax2.imshow(classification_map, alpha=0.7)
ax2.set_title("Classification Map", fontsize=14)
ax2.axis('off')

# Add legend
legend_elements = []
class_counts = {}
for result in results:
    class_counts[result['prediction']] = class_counts.get(result['prediction'], 0) + 1

# Sort by count
sorted_classes = sorted(class_counts.items(), key=lambda x: x[1], reverse=True)

for class_name, count in sorted_classes[:10]:  # Show top 10 classes
    color = color_map.get(class_name, '#808080')
    percentage = (count / len(results)) * 100
    legend_elements.append(mpatches.Patch(color=color, 
                                        label=f"{class_name} ({percentage:.1f}%)"))

ax2.legend(handles=legend_elements, loc='center left', bbox_to_anchor=(1, 0.5))

plt.suptitle(f"{analysis_name} Results", fontsize=16)
plt.tight_layout()
plt.savefig(os.path.join(output_folder, 'classification_overview.png'), dpi=150, bbox_inches='tight')
plt.close()

# Generate summary report
print("Generating summary report...")

summary = {
    "analysis_type": analysis_name,
    "model_type": model_type,
    "slide_path": svs_path,
    "slide_dimensions": {"width": slide_width, "height": slide_height},
    "analysis_parameters": {
        "patch_size": patch_size,
        "patch_stride": patch_stride,
        "total_patches": len(results),
        "max_patches": max_patches
    },
    "timestamp": datetime.now().isoformat(),
    "class_distribution": {},
    "high_confidence_regions": []
}

# Calculate class distribution
for class_name in class_names:
    count = sum(1 for r in results if r['prediction'] == class_name)
    percentage = (count / len(results)) * 100 if results else 0
    summary["class_distribution"][class_name] = {
        "count": count,
        "percentage": round(percentage, 2)
    }

# Find high-confidence regions for each significant class
for class_name, stats in summary["class_distribution"].items():
    if stats["percentage"] > 5:  # Only for classes with >5% presence
        high_conf_patches = [r for r in results 
                           if r['prediction'] == class_name and r['confidence'] > 0.8]
        
        if high_conf_patches:
            # Find centroid of high-confidence regions
            avg_x = sum(p['x'] for p in high_conf_patches) / len(high_conf_patches)
            avg_y = sum(p['y'] for p in high_conf_patches) / len(high_conf_patches)
            
            summary["high_confidence_regions"].append({
                "class": class_name,
                "patch_count": len(high_conf_patches),
                "centroid": {"x": int(avg_x), "y": int(avg_y)},
                "average_confidence": round(
                    sum(p['confidence'] for p in high_conf_patches) / len(high_conf_patches), 3
                )
            })

# Save summary
summary_path = os.path.join(output_folder, 'analysis_summary.json')
with open(summary_path, 'w') as f:
    json.dump(summary, f, indent=2)

# Generate HTML report
html_report = f"""
<!DOCTYPE html>
<html>
<head>
    <title>{analysis_name} Report</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; }}
        h1, h2 {{ color: #2c3e50; }}
        .overview {{ display: flex; gap: 20px; margin: 20px 0; }}
        .stat-box {{ background: #f0f0f0; padding: 15px; border-radius: 5px; }}
        .distribution-table {{ border-collapse: collapse; width: 100%; }}
        .distribution-table th, .distribution-table td {{ 
            border: 1px solid #ddd; padding: 8px; text-align: left; 
        }}
        .distribution-table th {{ background-color: #3498db; color: white; }}
        .color-box {{ display: inline-block; width: 20px; height: 20px; 
                     margin-right: 5px; vertical-align: middle; }}
        img {{ max-width: 100%; height: auto; }}
    </style>
</head>
<body>
    <h1>{analysis_name} Report</h1>
    <p>Generated on: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
    
    <div class="overview">
        <div class="stat-box">
            <h3>Slide Information</h3>
            <p>File: {os.path.basename(svs_path)}</p>
            <p>Dimensions: {slide_width} × {slide_height} pixels</p>
        </div>
        <div class="stat-box">
            <h3>Analysis Parameters</h3>
            <p>Model: {model_type.upper()}</p>
            <p>Patches analyzed: {len(results)}</p>
            <p>Patch size: {patch_size} × {patch_size} pixels</p>
            <p>Stride: {patch_stride} pixels</p>
        </div>
    </div>
    
    <h2>Classification Overview</h2>
    <img src="classification_overview.png" alt="Classification Overview">
    
    <h2>Class Distribution</h2>
    <table class="distribution-table">
        <tr>
            <th>Class</th>
            <th>Color</th>
            <th>Count</th>
            <th>Percentage</th>
        </tr>
"""

# Add class distribution to HTML
for class_name, stats in sorted(summary["class_distribution"].items(), 
                               key=lambda x: x[1]["percentage"], reverse=True):
    if stats["percentage"] > 0:
        color = color_map.get(class_name, '#808080')
        html_report += f"""
        <tr>
            <td>{class_name}</td>
            <td><span class="color-box" style="background-color: {color};"></span></td>
            <td>{stats["count"]}</td>
            <td>{stats["percentage"]:.1f}%</td>
        </tr>
        """

html_report += """
    </table>
    
    <h2>Class-Specific Heatmaps</h2>
    <img src="class_heatmaps.png" alt="Class Heatmaps">
    
    <h2>High-Confidence Regions</h2>
    <ul>
"""

# Add high-confidence regions
for region in summary["high_confidence_regions"]:
    html_report += f"""
        <li><strong>{region["class"]}</strong>: {region["patch_count"]} high-confidence patches 
            (avg. confidence: {region["average_confidence"]:.1%}) centered around 
            ({region["centroid"]["x"]}, {region["centroid"]["y"]})</li>
    """

html_report += """
    </ul>
</body>
</html>
"""

# Save HTML report
with open(os.path.join(output_folder, 'report.html'), 'w') as f:
    f.write(html_report)

# Clean up
slide.close()

print(f"\nAnalysis complete!")
print(f"Results saved to: {output_folder}")
print(f"- Classification overview: classification_overview.png")
print(f"- Class heatmaps: class_heatmaps.png")
print(f"- Summary data: analysis_summary.json")
print(f"- HTML report: report.html")
print(f"- Raw predictions: patch_predictions.json")