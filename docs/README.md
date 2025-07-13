# SPIDER QuPath Plugin - AI-Powered Digital Pathology Analysis

> **Quick Start:** Professional pathologists can start analyzing slides with AI in under 30 minutes!

[![Video Tutorial](https://img.shields.io/badge/📺_Video_Tutorial-Watch_on_YouTube-red?style=for-the-badge)](https://www.youtube.com/watch?v=8tLORQ7yUIQ)

## 🎯 What This Repository Offers

This is a complete **QuPath plugin** that brings state-of-the-art SPIDER AI models directly into your pathology workflow. No complex setup - just install, configure, and start classifying tissues with 90%+ accuracy.

### ✨ Key Benefits for Pathologists
- **🚀 One-click installation** - Works directly in QuPath
- **🎯 Three analysis modes** - From quick regions to whole slide analysis  
- **🧠 Expert-level AI** - 91-96% accuracy across tissue types
- **⚡ Real-time results** - Classifications appear instantly in QuPath
- **📊 Detailed reports** - Confidence scores, tissue composition, heatmaps

---

## 🚀 Quick Start (30 Minutes Setup)

### Step 1: Prerequisites (5 minutes)
- **QuPath 0.3.0+** - [Download here](https://qupath.github.io/)
- **Python 3.8+** - [Download here](https://www.python.org/downloads/)

### Step 2: Install Dependencies (10 minutes)
```bash
# Create isolated environment
python -m venv spider_env
spider_env\Scripts\activate  # Windows
# source spider_env/bin/activate  # Mac/Linux

# Install required packages
pip install torch transformers openslide-python pillow numpy matplotlib
```

### Step 3: Download AI Models (10 minutes)
Choose your tissue type and download from HuggingFace:
- 🟢 [**Colorectal Model**](https://huggingface.co/histai/SPIDER-colorectal-model) - 13 classes, 91.4% accuracy
- 🔵 [**Skin Model**](https://huggingface.co/histai/SPIDER-skin-model) - 24 classes, 94.0% accuracy  
- 🟡 [**Thorax Model**](https://huggingface.co/histai/SPIDER-thorax-model) - 14 classes, 96.2% accuracy

### Step 4: Install Plugin (5 minutes)
1. Open QuPath → **Automate** → **Show script editor**
2. Open and run: `scripts/spider_quick_installer.groovy`
3. Plugin appears under **Extensions** → **SPIDER Analysis** (Ctrl+Shift+S)

---

## 🎯 Three Analysis Workflows

### 1️⃣ **Quick Classification** ⚡
**Best for:** Specific regions of interest
1. Draw annotations around tissue areas
2. Select annotations → SPIDER menu → "Classify Selected Annotations"
3. Get instant AI classifications with confidence scores

### 2️⃣ **Detailed Tile Analysis** 🔍
**Best for:** Heterogeneous tissue composition
1. Draw large annotation over mixed tissue
2. SPIDER menu → "Tile Analysis (Detailed)"
3. See 1120×1120 pixel grid with individual classifications
4. Get percentage breakdown of tissue types

### 3️⃣ **Whole Slide Analysis** 🗺️
**Best for:** Complete slide overview and research
1. SPIDER menu → "Whole Slide Analysis"
2. Configure output directory and parameters
3. Get comprehensive heatmaps and HTML reports
4. Perfect for research and documentation

---

## 🏥 Clinical Use Cases

### **Diagnostic Pathology**
- **Tumor grading** - Distinguish high/low grade adenocarcinomas
- **Margin assessment** - Identify tumor vs normal tissue boundaries
- **Quality control** - Verify diagnostic accuracy with AI confidence scores

### **Research Applications**
- **Tissue quantification** - Automated measurement of tissue components
- **Biomarker studies** - Spatial analysis of tissue architecture
- **Training cases** - Educational tool for residents and fellows

### **Laboratory Workflow**
- **Screening assistance** - Pre-screen cases for pathologist review
- **Standardization** - Consistent classification across observers
- **Documentation** - Generate detailed reports with visual evidence

---

## 🧠 Available AI Models

| Model | Tissue Type | Classes | Accuracy | Key Features |
|-------|-------------|---------|----------|--------------|
| **Colorectal** | Colon/Rectum | 13 | 91.4% | Tumor grading, polyp classification |
| **Skin** | Dermatopathology | 24 | 94.0% | Melanoma, BCC, SCC detection |
| **Thorax** | Lung/Chest | 14 | 96.2% | Lung cancer classification |

### Colorectal Classes Include:
- Adenocarcinoma (high/low grade)
- Adenoma (high/low grade)  
- Hyperplastic polyp, Sessile serrated lesion
- Normal tissues: Fat, Muscle, Stroma, Vessels
- Pathological: Inflammation, Mucus, Necrosis

---

## ⚙️ Configuration Guide

After installation, configure the plugin:

1. **Open SPIDER menu** (Extensions → SPIDER Analysis)
2. **Click Configuration** ⚙️
3. **Set paths:**
   - **Python Path:** Point to your virtual environment python.exe
   - **Models Directory:** Folder containing downloaded models
4. **Test Configuration** to verify everything works

### Troubleshooting Configuration
- ❌ **Python not found:** Use full path to python.exe in virtual environment
- ❌ **Models not found:** Ensure folders named exactly `SPIDER-colorectal-model` etc.
- ❌ **Import errors:** Verify all pip packages installed correctly

---

## 📊 Understanding Results

### Classification Output
- **Color-coded annotations** - Each tissue type has consistent colors
- **Confidence scores** - Percentage confidence for each prediction
- **Alternative predictions** - See runner-up classifications
- **Spatial analysis** - Understand tissue distribution patterns

### Confidence Interpretation
- **>80%** - High confidence, likely accurate
- **60-80%** - Moderate confidence, review recommended  
- **<60%** - Low confidence, manual verification needed

### Quality Control Tips
- Use appropriate model for tissue type
- Draw annotations around homogeneous regions
- Review low-confidence results manually
- Compare AI results with clinical impression

---

## 🔬 Advanced Features

### Batch Processing
- Select multiple annotations for simultaneous classification
- Process entire slides automatically
- Export results to CSV/JSON for analysis

### Research Tools
- Generate publication-ready heatmaps
- Export detailed measurement data
- Create comprehensive HTML reports
- Track classification history

### Integration Options
- Works with existing QuPath workflows
- Compatible with QuPath's measurement tools
- Exports to standard pathology formats

---

## 📚 Learning Resources

- 📺 [**Video Tutorial**](https://www.youtube.com/watch?v=8tLORQ7yUIQ) - Complete walkthrough
- 📖 [**Setup Guide**](docs/SPIDER_QuPath_Plugin.md) - Detailed installation instructions
- 🔬 [**SPIDER Paper**](https://arxiv.org/abs/2503.02876) - Scientific background and validation

---

## 🆘 Common Issues & Solutions

### ❓ **"No SPIDER plugin in Extensions menu"**
**Solution:** Run `scripts/spider_plugin_menu.groovy` directly instead of installer

### ❓ **"Model not found" error**  
**Solution:** Verify model paths in configuration, use absolute paths

### ❓ **"Python script failed"**
**Solution:** Check Python environment, verify all packages installed

### ❓ **Slow processing**
**Solution:** Use GPU if available, reduce max_patches for large slides

### ❓ **Unexpected classifications**
**Solution:** Verify correct model for tissue type, check annotation quality

---

## 🏆 Why Choose SPIDER?

### **Proven Accuracy**
- Validated on large, expert-annotated datasets
- Published performance metrics (90%+ accuracy)
- Consistent results across different slides

### **Clinical Integration**
- Designed by pathologists for pathologists
- Fits seamlessly into existing workflows  
- Maintains pathologist oversight and control

### **Research Ready**
- Generates quantitative, reproducible results
- Suitable for peer-reviewed publications
- Supports large-scale studies

---

## 📞 Support & Citation

### **Get Help**
- 💬 **Issues:** [GitHub Issues](https://github.com/HistAI/SPIDER/issues)
- 📧 **Email:** dmitry@hist.ai, alex@hist.ai, kate@hist.ai
- 🌐 **Website:** [HistAI](https://histai.com)

### **Citation**
```bibtex
@misc{nechaev2025spider,
  title={SPIDER: A Comprehensive Multi-Organ Supervised Pathology Dataset and Baseline Models}, 
  author={Dmitry Nechaev and Alexey Pchelnikov and Ekaterina Ivanova},
  year={2025},
  eprint={2503.02876},
  archivePrefix={arXiv},
  url={https://arxiv.org/abs/2503.02876}
}
```

---

## 🎯 Ready to Start?

1. **⬇️ Download this repository**
2. **🔧 Follow the Quick Start guide above**  
3. **🧠 Download your first AI model**
4. **🚀 Start analyzing slides with AI!**

**Questions?** Check our [detailed setup guide](docs/SPIDER_QuPath_Plugin.md) or watch the [video tutorial](https://www.youtube.com/watch?v=8tLORQ7yUIQ).

---

*Transform your pathology workflow with AI - Professional accuracy, simplified integration.*

## Overview

SPIDER is a large, high-quality, multi-organ pathology dataset and collection of pretrained models designed for computational pathology. It covers multiple organ types including Skin, Colorectal, and Thorax tissues, with comprehensive class coverage for pathologically relevant structures. The models have been trained using a supervised approach with expert-validated annotations.

Key features:
- Pre-trained models for multiple organ types
- Expert-annotated patches with validated labels
- Support for processing whole slide images
- Integration with QuPath software for interactive analysis
- Classification with context-aware architecture (1120×1120 patch size)

## System Requirements

- **Operating System**: Windows, macOS, or Linux
- **Python**: 3.7 or higher
- **GPU**: NVIDIA GPU with CUDA support recommended for faster processing
- **RAM**: 16GB minimum, 32GB+ recommended for whole slide images
- **Disk Space**: 5GB for models and software, additional space for slide images
- **Software**: QuPath 0.3.0+ for integration features

## Installation

1. **Install Python and Required Libraries**:

   ```bash
   # Create a virtual environment (recommended)
   python -m venv venv
   
   # Activate the environment
   # On Windows:
   venv\Scripts\activate
   # On macOS/Linux:
   source venv/bin/activate
   
   # Install required packages
   pip install torch torchvision transformers openslide-python pillow numpy matplotlib
   ```

2. **Download SPIDER Models**:

   Download the SPIDER models from Hugging Face:
   - [SPIDER-Skin Model](https://huggingface.co/histai/SPIDER-skin-model)
   - [SPIDER-Colorectal Model](https://huggingface.co/histai/SPIDER-colorectal-model)
   - [SPIDER-Thorax Model](https://huggingface.co/histai/SPIDER-thorax-model)

   You can download directly from the website or use:
   ```bash
   git lfs install
   git clone https://huggingface.co/histai/SPIDER-colorectal-model
   ```

3. **Install OpenSlide**:

   OpenSlide is required to read whole slide images:
   - **Windows**: Download from [OpenSlide Windows binaries](https://openslide.org/download/)
   - **macOS**: `brew install openslide`
   - **Linux**: `apt-get install openslide-tools`

4. **Install QuPath**:

   Download and install QuPath from [https://qupath.github.io/](https://qupath.github.io/) if you plan to use the QuPath integration.

## QuPath Integration

SPIDER includes scripts for integrating with QuPath, a popular open-source software for digital pathology analysis. This allows you to use the SPIDER models directly within QuPath.

### Annotating Regions

1. **Open a slide in QuPath**:
   - Launch QuPath
   - Go to `File > Open...` and select your slide

2. **Create annotations**:
   - Use QuPath's annotation tools (e.g., Brush, Polygon, Rectangle) to draw regions of interest
   - For best results, focus on homogeneous regions that correspond to SPIDER model classes
   - You can draw multiple annotations on a slide for batch classification


### Running SPIDER Classification

There are two main workflows for using SPIDER with QuPath:

#### Method 1: Classify Individual Annotations

1. **Set up scripts**:
   - Copy `spider_classify_annotations.groovy` and `spider_qupath_classifier.py` to your QuPath project directory
   - Edit the paths in `spider_classify_annotations.groovy` to match your setup:
     ```groovy
     def pythonPath = "path/to/your/python"  // Update to your Python path
     def modelPath = "path/to/SPIDER-colorectal-model"  // Update to your model path
     ```

2. **Run the script**:
   - Select the annotations you want to classify
   - Go to `Automate > Show script editor`
   - Open `spider_classify_annotations.groovy`
   - Click the "Run" button

3. **View results**:
   - The script will apply SPIDER classifications to your selected annotations
   - Classes will be color-coded and displayed in QuPath

#### Method 2: Create Tile Grid (Detailed Analysis)

1. **Set up the script**:
   - Copy `spider_tile_classifier.groovy` and `spider_qupath_classifier.py` to your QuPath project directory
   - Edit the paths in `spider_tile_classifier.groovy` as in Method 1

2. **Run the script**:
   - Select a larger annotation to divide into tiles
   - Run the `spider_tile_classifier.groovy` script
   - This will divide your annotation into 1120×1120 pixel tiles and classify each one

3. **View the results**:
   - Each tile will be given a classification with abbreviated class names
   - The parent annotation will have measurements for each class percentage
   - This provides a detailed spatial analysis of tissue composition

### Interpretation of Results

- **Classification labels**: Each annotation/tile is assigned the most likely class
- **Confidence scores**: Available in the measurements tab (typically as percentages)
- **Class distribution**: For tile analysis, the parent annotation contains percentage measurements for each class
- **Color coding**: Classes are color-coded for easy visual interpretation

The detailed version (`spider_classify_annotations_detailed.groovy`) also shows alternative classifications when confidence is split among multiple classes.

## Whole Slide Analysis

The `whole_slide_analysis_spider_colorectal.py` script allows you to analyze entire slides without QuPath.

Usage:
```bash
python whole_slide_analysis_spider_colorectal.py <model_path> <svs_path> [output_folder] [patch_stride] [max_patches] [num_workers]
```

Example:
```bash
python whole_slide_analysis_spider_colorectal.py ./SPIDER-colorectal-model ./slides/example.svs ./output 560 1000 4
```

This will:
1. Process the slide using the specified model
2. Generate heatmaps for each class
3. Create a visualization showing the class distribution
4. Save the results to the output folder

There's also a GUI application (`spider_pathology_app.py`) for interactive slide analysis.

## Available Models

SPIDER includes models for different organ types:

### Colorectal Model
- 13 classes including:
  - Adenocarcinoma (high/low grade)
  - Adenoma (high/low grade)
  - Fat, Inflammation, Mucus, Muscle
  - Necrosis, Stroma, Vessels, etc.
- Accuracy: 91.4%

### Skin Model
- 24 classes including:
  - Basal Cell Carcinoma
  - Melanoma (invasive/in situ)
  - Squamous Cell Carcinoma
  - Normal structures (epidermis, vessels, etc.)
- Accuracy: 94.0%

### Thorax Model
- 14 classes including:
  - Tumor types (small cell, non-small cell)
  - Normal structures (alveoli, vessels, etc.)
- Accuracy: 96.2%

## Troubleshooting

**Issue**: "Model not found" error
- Ensure the model path is correct in your scripts
- Check if all model files are downloaded properly
- Try using absolute paths instead of relative paths

**Issue**: "CUDA out of memory" error
- Reduce `max_patches` parameter when analyzing large slides
- Use a smaller batch size
- Process the slide in multiple parts

**Issue**: Classification seems incorrect
- Ensure the correct model for the tissue type is being used
- Check if annotations are properly drawn around homogeneous regions
- Try using the context-aware mode (1120×1120 pixel regions)

**Issue**: Python errors in QuPath
- Verify your Python environment has all required dependencies
- Make sure paths in the script are correctly set
- Check console output for specific error messages

## Citation

If you use SPIDER in your research, please cite:

```bibtex
@misc{nechaev2025spidercomprehensivemultiorgansupervised,
      title={SPIDER: A Comprehensive Multi-Organ Supervised Pathology Dataset and Baseline Models}, 
      author={Dmitry Nechaev and Alexey Pchelnikov and Ekaterina Ivanova},
      year={2025},
      eprint={2503.02876},
      archivePrefix={arXiv},
      primaryClass={eess.IV},
      url={https://arxiv.org/abs/2503.02876}, 
}
```

## Support

For more information or support:
- GitHub Repository: [https://github.com/HistAI/SPIDER](https://github.com/HistAI/SPIDER)
- Hugging Face: [https://huggingface.co/histai](https://huggingface.co/histai)
- Contact: dmitry@hist.ai, alex@hist.ai, kate@hist.ai

---

## Step-by-Step Tutorial: Classifying Regions in QuPath

Here's a detailed walkthrough for first-time users:

### 1. Prepare Your Environment

1. Install QuPath and set up your Python environment as described in the Installation section
2. Download the SPIDER model for your tissue type (e.g., SPIDER-colorectal-model)
3. Create a new QuPath project and import your slide

### 2. Set Up the Scripts

1. Create a folder called `scripts` in your QuPath project directory
2. Copy the following files from the SPIDER package:
   - `spider_classify_annotations.groovy` (or the detailed version)
   - `spider_qupath_classifier.py`
3. Open `spider_classify_annotations.groovy` in a text editor
4. Update these paths at the top of the file:
   ```groovy
   def pythonPath = "C:\\path\\to\\python.exe"  // Your Python executable
   def outputPath = "C:\\path\\to\\output"      // Where to save results
   def scriptPath = "C:\\path\\to\\spider_qupath_classifier.py"
   def modelPath = "C:\\path\\to\\SPIDER-colorectal-model"
   ```

### 3. Create Annotations in QuPath

1. Select an annotation tool from the toolbar (e.g., Brush, Polygon)
2. Draw regions around areas of interest:
   - For the colorectal model, try to isolate regions like tumor areas, stroma, muscle, etc.
   - Make several annotations for different tissue types
3. Select all the annotations you want to classify

### 4. Run the Classification

1. In QuPath, go to `Automate > Show script editor`
2. Click `File > Open...` and select your `spider_classify_annotations.groovy` script
3. Click the "Run" button (triangle icon)
4. The script will:
   - Export your annotations to a temporary JSON file
   - Call the Python script to run the SPIDER model
   - Import the results back into QuPath
   - Apply classifications to your annotations

### 5. Interpret the Results

1. Annotations will be colored according to their classification
2. Click on an annotation to see details in the Annotations panel:
   - The class name will be shown
   - The confidence score is available in the measurements tab
   - For the detailed version, alternative classifications will be listed

### 6. For Detailed Analysis with Tiles

Follow the same procedure, but use `spider_tile_classifier.groovy` instead. This will:
1. Divide your annotations into 1120×1120 pixel tiles
2. Classify each tile individually
3. Show a detailed breakdown of tissue composition

This is especially useful for heterogeneous regions containing multiple tissue types.

### 7. Saving and Exporting Results

1. Save your QuPath project with `File > Save`
2. Export annotations with `File > Export > Annotations...`
3. For heatmap visualizations, run the `whole_slide_analysis_spider_colorectal.py` script separately

---

If you need further help or have questions, please reach out to the SPIDER team at the contact information provided in the Support section.