# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a QuPath plugin for SPIDER (Supervised Pathology Image-DEscription Repository) digital pathology analysis. The SPIDER models are pre-trained deep learning models for classifying pathology images across multiple organ types (Colorectal, Skin, Thorax) with high accuracy.

## Project Structure

```
spider-qupath/
├── scripts/                     # QuPath Groovy scripts
│   ├── spider_plugin_menu.groovy      # Main plugin menu and GUI
│   ├── spider_classify_annotations.groovy  # Basic annotation classifier
│   └── spider_tile_classifier.groovy      # Tile-based analysis
├── python/                      # Python SPIDER model interfaces
│   ├── spider_qupath_classifier_universal.py  # Universal classifier
│   ├── spider_qupath_classifier.py            # Basic classifier
│   └── whole_slide_analysis_spider_universal.py  # Full slide analysis
├── config/                      # Configuration files
│   └── spider_config.json       # User settings (Python path, model paths)
├── output/                      # Analysis results directory
│   ├── classifications/         # Annotation classification results
│   ├── tiles/                  # Tile analysis results
│   └── whole_slide/            # Whole slide analysis outputs
├── docs/                       # Comprehensive documentation
│   ├── README.md               # Complete usage guide and tutorial
│   └── SPIDER_QuPath_Plugin.md # Installation and setup guide
└── data/                       # QuPath project data (auto-generated)
```

## Key Technologies

- **QuPath**: Open-source digital pathology platform
- **Groovy**: QuPath scripting language for GUI and automation
- **Python**: SPIDER model inference (PyTorch, Transformers, OpenSlide)
- **SPIDER Models**: Pre-trained deep learning models from HistAI
  - Colorectal: 13 classes, 91.4% accuracy
  - Skin: 24 classes, 94.0% accuracy  
  - Thorax: 14 classes, 96.2% accuracy

## Core Workflows

### 1. Classify Selected Annotations
- User draws annotations in QuPath
- Script exports ROI coordinates to JSON
- Python classifier runs SPIDER model on extracted regions
- Results imported back as QuPath classifications with confidence scores

### 2. Tile Analysis (Detailed)
- Divides annotations into 1120×1120 pixel tiles
- Classifies each tile individually
- Shows spatial distribution of tissue types
- Provides percentage composition statistics

### 3. Whole Slide Analysis
- Processes entire slide without manual annotations
- Generates heatmaps for each tissue class
- Creates comprehensive HTML reports with visualizations
- Configurable patch stride and memory limits

## Development Commands

**Run QuPath Scripts:**
- Open QuPath GUI and use Script Editor
- Scripts located in `scripts/` directory
- Main entry point: `spider_plugin_menu.groovy`

**Test Python Components:**
```bash
# Activate virtual environment with SPIDER dependencies
source venv/bin/activate  # or venv\Scripts\activate on Windows

# Test universal classifier
python python/spider_qupath_classifier_universal.py \
    output/classifications/annotations_to_predict.json \
    /path/to/SPIDER-colorectal-model \
    output/classifications/

# Test whole slide analysis
python python/whole_slide_analysis_spider_universal.py \
    /path/to/model \
    /path/to/slide.svs \
    output/whole_slide/ \
    560 1000 4
```

**Configuration Management:**
- User settings stored in `config/spider_config.json`
- Contains Python executable path and model directory paths
- Auto-saved through QuPath configuration dialog

## Architecture Notes

### QuPath Integration
- **Plugin Menu**: JavaFX-based GUI accessible via Extensions menu (Ctrl+Shift+S)
- **Configuration System**: Persistent JSON config with file browser dialogs
- **Multi-model Support**: Dynamic model selection with appropriate class mappings
- **Error Handling**: Comprehensive validation of paths, models, and file formats

### Python Bridge
- **Process Isolation**: Python scripts run as separate processes to avoid JVM conflicts
- **JSON Communication**: All data exchange via structured JSON files
- **Path Parsing**: Handles QuPath's complex URI formats for slide files
- **Model Auto-detection**: Identifies model type from directory names

### Data Flow
1. **Export Phase**: QuPath exports annotation ROIs to JSON with slide paths
2. **Processing Phase**: Python extracts regions with 1120×1120 context windows
3. **Inference Phase**: SPIDER models process regions using HuggingFace transformers
4. **Import Phase**: Results parsed back into QuPath with classifications and measurements

## Important Implementation Details

### Path Handling
- QuPath uses complex server URIs (OpenslideImageServer:, BioFormatsImageServer:)
- Python scripts include robust path parsing for cross-platform compatibility
- Windows multiprocessing issues handled by forcing single-threaded execution

### Memory Management
- Configurable limits for whole slide analysis (`max_patches` parameter)
- GPU acceleration when available, CPU fallback
- Tile-based processing to handle large slides

### Class Color Schemes
- Model-specific color mappings for consistent visualization
- Abbreviated class names for tile display
- Confidence scores displayed as percentages

## Testing Approach

**Manual Testing:**
- Use QuPath GUI with test slides
- Verify all three workflows with different model types
- Check output files and QuPath annotations

**Error Testing:**
- Invalid model paths
- Missing Python dependencies
- Corrupted slide files
- Memory constraints

## Dependencies

**QuPath Requirements:**
- QuPath 0.3.0+
- Java 8+

**Python Requirements:**
```bash
pip install torch torchvision transformers openslide-python pillow numpy matplotlib
```

**SPIDER Models:**
- Download from HuggingFace: histai/SPIDER-{colorectal,skin,thorax}-model
- Models are ~1-2GB each
- Require `trust_remote_code=True` for custom architectures

## Common Issues

1. **Model Loading Errors**: Verify HuggingFace model downloads are complete
2. **OpenSlide Issues**: Ensure OpenSlide binary installation on system PATH
3. **Memory Errors**: Reduce max_patches or use GPU acceleration
4. **Path Errors**: Use absolute paths in configuration
5. **Windows Multiprocessing**: Plugin automatically forces single-threaded mode

## File Naming Conventions

- **Groovy Scripts**: `snake_case.groovy`
- **Python Scripts**: `snake_case.py` 
- **Output Files**: `descriptive_name.json/png/html`
- **Configuration**: `spider_config.json`

The codebase follows QuPath conventions with Groovy for GUI/automation and Python for ML inference, providing a seamless bridge between digital pathology workflows and state-of-the-art AI models.