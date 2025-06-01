# SPIDER QuPath Plugin - Complete Installation & Setup Guide

## Overview

This guide will walk you through setting up the SPIDER pathology analysis plugin for QuPath, allowing you to use AI-powered tissue classification on your whole slide images.

## What You'll Be Able to Do

- **Classify tissue regions** using state-of-the-art SPIDER models
- **Choose between models** for different tissue types (Colorectal, Skin, Thorax)
- **Three analysis modes**:
  1. Quick classification of selected regions
  2. Detailed tile-based analysis
  3. Whole slide analysis with heatmaps

---

## Step 1: Prerequisites

### 1.1 Install QuPath
- Download QuPath from: https://qupath.github.io/
- Version 0.3.0 or higher recommended
- Install and verify it opens correctly

### 1.2 Install Python
- Download Python 3.8 or higher from: https://www.python.org/downloads/
- During installation, **check "Add Python to PATH"**
- Verify installation:
  ```bash
  python --version
  ```

### 1.3 Install Git (for downloading models)
- Download from: https://git-scm.com/downloads
- Install Git LFS after Git:
  ```bash
  git lfs install
  ```

---

## Step 2: Set Up Python Environment

Open a terminal/command prompt and run:

```bash
# Create a virtual environment
python -m venv spider_env

# Activate it
# On Windows:
spider_env\Scripts\activate
# On macOS/Linux:
source spider_env/bin/activate

# Install required packages
pip install torch torchvision transformers openslide-python pillow numpy matplotlib
```

### 2.1 Install OpenSlide (required for reading slides)

**Windows:**
1. Download OpenSlide Windows binaries from: https://openslide.org/download/
2. Extract to a folder (e.g., `C:\openslide`)
3. Add the `bin` folder to your system PATH

**macOS:**
```bash
brew install openslide
```

**Linux:**
```bash
sudo apt-get install openslide-tools
```

---

## Step 3: Download SPIDER Models

Create a folder for the models (e.g., `D:\SPIDER_Models`) and download:

### Option A: Using Git (Recommended)
```bash
cd D:\SPIDER_Models

# Colorectal Model
git clone https://huggingface.co/histai/SPIDER-colorectal-model

# Skin Model
git clone https://huggingface.co/histai/SPIDER-skin-model

# Thorax Model
git clone https://huggingface.co/histai/SPIDER-thorax-model
```

### Option B: Manual Download
Visit each link and click "Files and versions" to download all files:
- [Colorectal Model](https://huggingface.co/histai/SPIDER-colorectal-model)
- [Skin Model](https://huggingface.co/histai/SPIDER-skin-model)
- [Thorax Model](https://huggingface.co/histai/SPIDER-thorax-model)

---

## Step 4: Install SPIDER Plugin in QuPath

### 4.1 Create Project Structure

1. Open QuPath and create a new project
2. In your project folder, create a `scripts` subfolder
3. Copy these files to the scripts folder:
   - `SPIDER_Plugin_Menu.groovy` (main menu)
   - `spider_qupath_classifier_universal.py` (classifier)
   - `whole_slide_analysis_spider_universal.py` (whole slide analysis)

### 4.2 Install the Plugin

1. In QuPath, go to **Automate → Show script editor**
2. Open `SPIDER_Plugin_Menu.groovy`
3. Click **Run**
4. The SPIDER menu will appear under **Extensions → SPIDER Analysis**

---

## Step 5: Configure the Plugin

### 5.1 Initial Configuration

1. Open the SPIDER menu (**Extensions → SPIDER Analysis** or press **Ctrl+Shift+S**)
2. Click **⚙ Configuration**
3. Set your paths:
   - **Python Path**: Browse to your python.exe (e.g., `spider_env\Scripts\python.exe`)
   - **Models Directory**: Browse to your models folder (e.g., `D:\SPIDER_Models`)
4. Click **Test Configuration** to verify everything is working

### 5.2 Troubleshooting Configuration

If the test fails:
- **Python not found**: Make sure you're pointing to the python.exe in your virtual environment
- **Models not found**: Ensure model folders are named exactly as downloaded (e.g., `SPIDER-colorectal-model`)

---

## Step 6: Using the Plugin

### Workflow 1: Classify Selected Annotations

**Best for:** Quick classification of specific regions

1. Open your slide in QuPath
2. Draw annotations around regions of interest using:
   - **Brush tool** for irregular shapes
   - **Rectangle/Ellipse** for regular shapes
   - **Polygon** for precise boundaries
3. Select the annotations you want to classify
4. Open SPIDER menu and choose your model (Colorectal/Skin/Thorax)
5. Click **"1. Classify Selected Annotations"**
6. Wait for processing (check the log for progress)
7. Results appear as:
   - Colored classifications
   - Confidence scores in the annotation name
   - Detailed measurements in the Annotations tab

### Workflow 2: Tile Analysis (Detailed)

**Best for:** Analyzing tissue composition in heterogeneous regions

1. Draw a larger annotation covering mixed tissue types
2. Select the annotation
3. Open SPIDER menu and choose your model
4. Click **"2. Tile Analysis (Detailed)"**
5. The annotation will be divided into 1120×1120 pixel tiles
6. Each tile gets classified individually
7. View results:
   - Individual tile classifications with abbreviated names
   - Overall tissue composition percentages in the parent annotation

### Workflow 3: Whole Slide Analysis

**Best for:** Complete slide overview and heatmap generation

1. Open your slide (no annotations needed)
2. Open SPIDER menu and choose your model
3. Click **"3. Whole Slide Analysis"**
4. Configure settings:
   - **Output Directory**: Where to save results
   - **Patch Stride**: 560 (50% overlap) is recommended
   - **Max Patches**: Limit based on your computer's memory
   - **Workers**: Number of parallel processes (4-8 recommended)
5. Click OK and wait for processing
6. Find results in the output directory:
   - `classification_overview.png`: Overall tissue map
   - `class_heatmaps.png`: Individual class probability maps
   - `report.html`: Complete analysis report

---

## Step 7: Interpreting Results

### Understanding Classifications

Each model provides different tissue classifications:

**Colorectal Model (13 classes):**
- Tumor types: Adenocarcinoma (high/low grade)
- Precursors: Adenoma (high/low grade), Hyperplastic polyp
- Normal: Fat, Muscle, Stroma, Vessels
- Other: Inflammation, Mucus, Necrosis

**Skin Model (24 classes):**
- Malignant: BCC, SCC, Melanoma (invasive/in situ)
- Normal structures: Epidermis, Dermis, Hair follicles
- Other pathology classifications

**Thorax Model (14 classes):**
- Tumors: Small cell, Non-small cell carcinoma
- Normal: Alveoli, Bronchus, Vessels
- Other tissue types

### Confidence Scores

- **>80%**: High confidence - likely accurate
- **60-80%**: Moderate confidence - review recommended
- **<60%**: Low confidence - manual verification needed

### Color Legend

Each class has a consistent color across all visualizations:
- Reds: Usually tumors/carcinomas
- Greens: Normal stroma/tissue
- Blues: Vessels/fluids
- Yellows: Fat/inflammation

---

## Step 8: Tips for Best Results

### Annotation Tips

1. **For classification**: Draw annotations around homogeneous tissue
2. **For tile analysis**: Include diverse tissue in one annotation
3. **Size matters**: Very small annotations may lack context
4. **Multiple selections**: Process multiple annotations at once

### Performance Tips

1. **Start small**: Test on a few annotations first
2. **GPU acceleration**: If available, significantly speeds up processing
3. **Memory management**: For whole slide analysis, adjust max_patches if running out of memory
4. **Batch processing**: Select multiple annotations for efficiency

### Quality Control

1. **Review low-confidence results**: Check annotations with <70% confidence
2. **Context matters**: The 1120×1120 context window helps accuracy
3. **Model selection**: Use the appropriate model for your tissue type
4. **Compare methods**: Try both individual annotation and tile analysis

---

## Troubleshooting

### Common Issues and Solutions

**"Model not found" error:**
- Check model path in configuration
- Ensure model folders contain all required files
- Model folder names must match exactly

**"CUDA out of memory" error:**
- Reduce max_patches for whole slide analysis
- Process fewer annotations at once
- Close other applications

**Classifications seem incorrect:**
- Verify you're using the correct model for your tissue
- Check annotation quality (clear, focused regions)
- Review confidence scores

**Python errors in QuPath:**
- Verify Python environment is activated
- Check all packages are installed
- Look at the full error message in the log

**Slow processing:**
- Normal for CPU processing (consider GPU)
- Reduce number of patches for testing
- Check system resources

---

## Additional Resources

- **SPIDER Paper**: https://arxiv.org/abs/2503.02876
- **QuPath Documentation**: https://qupath.readthedocs.io/
- **Support Contact**: dmitry@hist.ai, alex@hist.ai, kate@hist.ai

---

## Quick Reference Card

### Keyboard Shortcuts
- **Ctrl+Shift+S**: Open SPIDER menu
- **B**: Brush tool for annotations
- **M**: Move tool
- **V**: Selection tool

### Workflow Decision Tree
```
What do you want to analyze?
├── Specific regions → Use "Classify Annotations"
├── Tissue composition → Use "Tile Analysis"
└── Entire slide → Use "Whole Slide Analysis"
```

### Model Selection Guide
```
What tissue type?
├── Colon/Rectum → Colorectal Model
├── Skin → Skin Model
└── Lung/Chest → Thorax Model
```

---

## Updates and Version Notes

- **Version 1.0**: Initial release with three SPIDER models
- **Future updates**: Check the GitHub repository for new models and features

Remember to cite the SPIDER paper if using these tools for research!