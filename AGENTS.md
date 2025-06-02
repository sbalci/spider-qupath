# AGENTS.md

## Table of Contents

1. [Introduction](#introduction)
2. [Prerequisites](#prerequisites)
3. [Agent Architecture and Conventions](#agent-architecture-and-conventions)

   * [Agent Concept](#agent-concept)
   * [Naming Conventions](#naming-conventions)
   * [Directory Structure](#directory-structure)
4. [QuPath Scripting Fundamentals](#qupath-scripting-fundamentals)

   * [QP API Overview](#qp-api-overview)
   * [QPEx API Overview](#qpex-api-overview)
   * [Command Line Usage](#command-line-usage)
   * [Gradle Scripting Project Structure](#gradle-scripting-project-structure)
5. [Standard Agents](#standard-agents)

   * [1. ProjectManagerAgent](#1-projectmanageragent)
   * [2. ImageImportAgent](#2-imageimportagent)
   * [3. AnnotationAgent](#3-annotationagent)
   * [4. TilingAgent](#4-tilingagent)
   * [5. ModelInferenceAgent](#5-modelinferenceagent)
   * [6. PostProcessingAgent](#6-postprocessingagent)
   * [7. ExportAgent](#7-exportagent)
   * [8. QC\_Agent](#8-qc_agent)
   * [9. ReportingAgent](#9-reportingagent)
6. [Creating Custom Agents](#creating-custom-agents)

   * [Defining a New Agent](#defining-a-new-agent)
   * [Agent Interface and Base Class](#agent-interface-and-base-class)
   * [Registering Agents in the Pipeline](#registering-agents-in-the-pipeline)
7. [Agent Lifecycle and Execution Flow](#agent-lifecycle-and-execution-flow)

   * [Initialization](#initialization)
   * [Execution](#execution)
   * [Error Handling](#error-handling)
   * [Cleanup](#cleanup)
8. [Configuration and Parameters](#configuration-and-parameters)

   * [YAML/JSON Config Files](#yamljson-config-files)
   * [Runtime Arguments](#runtime-arguments)
9. [Best Practices](#best-practices)

   * [Modularity](#modularity)
   * [Logging](#logging)
   * [Performance Considerations](#performance-considerations)
   * [Version Control](#version-control)
10. [Examples and Use Cases](#examples-and-use-cases)

    * [Example 1: Batch Annotation Workflow](#example-1-batch-annotation-workflow)
    * [Example 2: Tile-Based Model Inference](#example-2-tile-based-model-inference)
    * [Example 3: Quality Control Dashboard Export](#example-3-quality-control-dashboard-export)
11. [Troubleshooting and FAQs](#troubleshooting-and-faqs)

    * [Common Errors](#common-errors)
    * [Debugging Tips](#debugging-tips)
12. [References and Further Reading](#references-and-further-reading)

---

## Introduction

QuPath is a leading open-source platform for digital pathology, offering extensive scripting capabilities via Groovy. As projects grow in scale and complexity, adopting an **agent-based**, modular approach to scripting can greatly improve maintainability and reusability. This document describes how to create and integrate **agents**—self-contained Groovy classes or scripts that each perform a specific task—into QuPath workflows.

Each agent encapsulates discrete functionality (e.g., importing images, detecting annotations, running inference) and follows a standardized interface with methods such as `initialize()`, `execute()`, and `cleanup()`. By chaining agents together, you can assemble robust pipelines that are easier to debug and extend.

For the most up-to-date list of functions and arguments, consult the official Javadoc index: [https://qupath.github.io/javadoc/docs/index-all.html](https://qupath.github.io/javadoc/docs/index-all.html).

---

## Prerequisites

Before building or using agents, ensure that you have:

1. **QuPath Installation**: QuPath v0.3.x or later, with scripting enabled.
2. **Java and Groovy**: Java 8+ installed. Groovy is embedded within QuPath, so no separate Groovy installation is required.
3. **IDE or Editor**: A text editor or IDE with Groovy support (e.g., IntelliJ IDEA, VS Code with Groovy extensions).
4. **Git and Gradle**: For version control and optional build automation. Gradle can manage dependencies and build scripts (see [Gradle Scripting Project Structure](#gradle-scripting-project-structure)).
5. **Optional Dependencies**:

   * **ImageJ** for advanced image processing.
   * **ONNX Runtime**, **TensorFlow Java**, or other inference engines for model-based agents.
   * **Apache PDFBox** for generating PDF reports.

---

## Agent Architecture and Conventions

### Agent Concept

An **agent** is a Groovy class (or script) responsible for a single, well-defined task within your pipeline. Each agent implements the following methods:

* `initialize(Map<String,Object> config)`: Perform setup tasks such as loading configuration parameters, creating output directories, loading models, or initializing loggers. This method is called once per pipeline run.
* `execute()`: Contain the core functionality of the agent (e.g., importing images, detecting objects, running inference). This is where the agent does its primary work.
* `cleanup()`: Optional. Release resources, close sessions, save state, or flush logs. Called at the end of the pipeline or if an error occurs.
* `getName()`: Return a unique agent name used to look up its configuration in the pipeline YAML/JSON file.

Agents are instantiated and invoked sequentially (or in a custom order) by a central orchestration script (e.g., `runPipeline.groovy`). This structure promotes modularity and makes it easy to add, remove, or reorder agents.

### Naming Conventions

* **File Names**: Each agent resides in its own file named `<AgentName>Agent.groovy` (e.g., `AnnotationAgent.groovy`).
* **Class Names**: Match the file name exactly (e.g., `class AnnotationAgent`).
* **Package**: Use a consistent base package such as `com.yourorganization.qupath.agents`.
* **Method Signatures**: Follow the standardized interface (`initialize()`, `execute()`, `cleanup()`, `getName()`).

### Directory Structure

A recommended project layout is:

```
<project-root>/
│
├── agents/                        # Groovy scripts defining each agent
│   ├── ProjectManagerAgent.groovy
│   ├── ImageImportAgent.groovy
│   ├── AnnotationAgent.groovy
│   ├── TilingAgent.groovy
│   ├── ModelInferenceAgent.groovy
│   ├── PostProcessingAgent.groovy
│   ├── QC_Agent.groovy
│   ├── ExportAgent.groovy
│   └── ReportingAgent.groovy
│
├── configs/                       # Configuration files (YAML/JSON)
│   └── pipeline.yaml
│
├── models/                        # Pre-trained models (ONNX, TensorFlow, etc.)
│   └── TumorClassifier.onnx
│
├── scripts/                       # Orchestration script and utility functions
│   ├── runPipeline.groovy
│   └── utils.groovy               # Shared helper methods
│
├── outputs/                       # All generated outputs
│   ├── annotations/
│   ├── tiles/
│   ├── results/
│   └── reports/
│
├── gradle/                        # (Optional) Gradle build scripts
│   └── build.gradle
└── AGENTS.md                      # This documentation file
```

---

## QuPath Scripting Fundamentals

This section summarizes essential QuPath scripting resources to help you develop agents.

### QP API Overview

The [QP API](https://qupath.github.io/javadoc/docs/qupath/lib/scripting/QP.html#method-summary) provides core scripting functions. Key methods include:

* `QP.getProject()`: Access the current QuPath project (`QuPathProject`).
* `QP.getCurrentImageData()`: Retrieve the `ImageData` object for the active image.
* `QP.getServer()`: Obtain the `ImageServer` for the current image, enabling operations at the pixel/server level.
* `QP.runCommand(String moduleName, String command, Map<String,Object> options)`: Execute built-in QuPath commands (e.g., cell detection, pixel classification).
* `QP.log(String message)`: Write a line to QuPath’s log console.
* `QP.exit()`: Exit QuPath programmatically.

Always consult the latest Javadoc for updated signatures and new methods: [https://qupath.github.io/javadoc/docs/index-all.html](https://qupath.github.io/javadoc/docs/index-all.html).

#### Example: Load and Annotate

```groovy
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.roi.ROIs

// Acquire the current image data
def imageData = QP.getCurrentImageData()

// Create a rectangular ROI at (100,100) with width=500 and height=500
def roi = ROIs.createRectangleROI(100, 100, 500, 500, ImagePlane.getDefaultPlane())

// Create a new annotation object from the ROI
def annotation = PathAnnotationObject.createAnnotationObject(roi)

// Add the annotation to the image hierarchy
QP.getProjectEntry().getHierarchy().addPathObject(annotation)

// Log the event
QP.log("Added a rectangular annotation to the current image.")
```

### QPEx API Overview

The [QPEx API](https://qupath.github.io/javadoc/docs/qupath/lib/gui/scripting/QPEx.html#method-summary) extends QP with methods focused on the GUI and project management. Essential methods include:

* `QPEx.selectProjectEntry()`: Open a dialog to select a QuPath project.
* `QPEx.openProject(String path)`: Programmatically open an existing QuPath project (`.qpj`).
* `QPEx.saveProject()`: Save the current project state.
* `QPEx.getSelectedObjects()`: Return a list of user-selected `PathObject`s in the GUI.
* `QPEx.getCurrentHierarchy()`: Access the `PathObjectHierarchy` for annotations, detections, etc.
* `QPEx.getAnnotationObjects()`: Fetch all annotation objects currently present in the active image.

Again, check the official Javadoc for the most recent additions: [https://qupath.github.io/javadoc/docs/index-all.html](https://qupath.github.io/javadoc/docs/index-all.html).

#### Example: Iterate Over All Annotations

```groovy
// Get the hierarchy of PathObjects for the active image
def hierarchy = QPEx.getCurrentHierarchy()

// Filter to only annotation objects
def annotations = hierarchy.getFlattenedObjectList(null).findAll { it.isAnnotation() }

// Log each annotation’s name and area
annotations.each { ann ->
    QP.log("Annotation: ${ann.getName()}, Area: ${ann.getROI().getArea()}")
}
```

### Command Line Usage

QuPath supports headless execution via the command line, which is critical for running agents in automated environments such as servers or CI/CD pipelines. See the [Command Line Guide](https://qupath.readthedocs.io/en/stable/docs/advanced/command_line.html).

#### Common CLI Options:

```
qupath Command Line Usage:
    -l <log level>       : Set log level (DEBUG, INFO, WARN)
    -q                   : Suppress system messages
    -p <project>         : Path to a QuPath project file (.qpj)
    -i <image>           : Path to a specific image file
    -s <script>          : Path to a Groovy script to run
    --data <folder>      : Set QuPath data directory
    -- /noplugin         : Disable all plugins
```

##### Example: Headless Script Execution

```bash
qupath -p /path/to/project.qpj -s /path/to/agents/ImageImportAgent.groovy -l INFO
```

You can chain multiple invocations or run a single orchestration script (e.g., `runPipeline.groovy`) to execute an entire pipeline.

### Gradle Scripting Project Structure

QuPath’s [Gradle Scripting Project](https://github.com/qupath/qupath-gradle-scripting-project) provides a template to manage dependencies and build Groovy scripts using QuPath’s classpath. Key points:

* **build.gradle**: Declare dependencies on QuPath core, GUI, and scripting JARs.
* **settings.gradle**: Set the project name and modules.
* **src/main/groovy/**: Place agent scripts and utility classes for compilation.
* **Application Plugin**: Use the `application` plugin to define a `mainClass` (e.g., a runner that orchestrates agents).

#### Minimal build.gradle Example:

```gradle
plugins {
    id 'application'
    id 'groovy'
}

group 'com.yourorganization'
version '1.0-SNAPSHOT'

def qupathHome = System.getenv('QU_PATH') ?: '/Applications/QuPath.app/Contents/Java'

repositories {
    mavenCentral()
}

dependencies {
    implementation files("$qupathHome/lib/qupath-core-0.3.2.jar")
    implementation files("$qupathHome/lib/qupath-gui-0.3.2.jar")
    implementation files("$qupathHome/lib/qupath-scripting-0.3.2.jar")
    implementation 'org.slf4j:slf4j-api:1.7.36'
    implementation 'ch.qos.logback:logback-classic:1.2.11'
    // Add other dependencies (e.g., ONNX Runtime) as needed
}

application {
    mainClass = 'com.yourorganization.qupath.Runner'
}
```

With this setup, running `gradle run` will launch the defined `Runner` class against QuPath’s libraries.

---

## Standard Agents

Below are nine recommended agents with detailed descriptions and code templates. Each agent follows the conventions outlined above.

### 1. ProjectManagerAgent

**Purpose**: Initialize project-wide resources, create directories, parse pipeline configurations, and set up logging.

**Responsibilities**:

* Create and verify output directories.
* Load and validate the main `pipeline.yaml` configuration.
* Initialize a logger (e.g., SLF4J with Logback).
* Open an existing QuPath project or create a new one via `QPEx`.

```groovy
package com.yourorganization.qupath.agents

import groovy.yaml.YamlSlurper
import qupath.lib.gui.scripting.QPEx
import org.slf4j.LoggerFactory

class ProjectManagerAgent {
    String name = 'ProjectManagerAgent'
    Map<String, Object> config
    def logger

    void initialize(Map<String, Object> config) {
        this.config = config
        logger = LoggerFactory.getLogger(ProjectManagerAgent)
        logger.info("[${name}] Initializing project...")

        // Create output directories
        config.outputDirs.each { String dirPath ->
            File dir = new File(dirPath)
            if (!dir.exists()) {
                dir.mkdirs()
                logger.info("[${name}] Created directory: ${dirPath}")
            }
        }

        // Open or create the QuPath project
        if (config.projectPath) {
            def projectFile = new File(config.projectPath)
            if (projectFile.exists()) {
                QPEx.openProject(config.projectPath)
                logger.info("[${name}] Opened QuPath project: ${config.projectPath}")
            } else {
                logger.info("[${name}] Creating new QuPath project: ${config.projectPath}")
                QPEx.createProject(config.projectPath)
            }
        }
    }

    void execute() {
        // No main execution logic; this agent handles initialization only.
    }

    void cleanup() {
        logger.info("[${name}] Cleanup complete. Saving project...")
        QPEx.saveProject()
    }

    String getName() { return name }
}
```

### 2. ImageImportAgent

**Purpose**: Locate and import whole-slide images (WSIs) from a specified directory into QuPath.

**Responsibilities**:

* Scan the given directory for supported image formats (e.g., `.svs`, `.tiff`, `.ndpi`, `.ome.tif`).
* Use `ServerTools.getSupportedServer(Path)` to load each image’s `ImageServer`.
* Optionally add each image’s `ImageData` to the QuPath project via `QPEx.addImageData()`.
* Log any unsupported formats or errors.

```groovy
package com.yourorganization.qupath.agents

import qupath.lib.images.servers.ServerTools
import qupath.lib.gui.scripting.QPEx
import qupath.lib.images.servers.ImageServer
import org.slf4j.LoggerFactory

class ImageImportAgent {
    String name = 'ImageImportAgent'
    Map<String, Object> config
    List<ImageServer> imageServers = []
    def logger

    void initialize(Map<String, Object> config) {
        this.config = config
        logger = LoggerFactory.getLogger(ImageImportAgent)
        logger.info("[${name}] Importing images from ${config.imageDirectory}")
    }

    void execute() {
        File imgDir = new File(config.imageDirectory)
        imgDir.eachFileMatch(~/(?i).*\.(svs|tiff|ndpi|ome\.tif)$/) { File imgFile ->
            logger.info("[${name}] Loading image: ${imgFile.name}")
            try {
                def server = ServerTools.getSupportedServer(imgFile.toPath())
                if (server) {
                    imageServers << server
                    // Add image to the current QuPath project
                    QPEx.addImageData(server.readImageData(), true)
                    logger.info("[${name}] Successfully loaded into project: ${imgFile.name}")
                } else {
                    logger.warn("[${name}] Unsupported image format: ${imgFile.name}")
                }
            } catch (Exception e) {
                logger.error("[${name}] Error loading ${imgFile.name}: ${e.message}")
            }
        }
    }

    void cleanup() {
        logger.info("[${name}] Completed image import. Total images imported: ${imageServers.size()}")
    }

    String getName() { return name }
}
```

### 3. AnnotationAgent

**Purpose**: Automate the creation of annotations using QuPath’s built-in detection commands or custom logic.

**Responsibilities**:

* Iterate over all images in the project.
* For each image, use `QP.runCommand()` to trigger a detection module (e.g., pixel classification, cell detection).
* Optionally create custom ROIs programmatically and add them via `PathAnnotationObject`.
* Save annotations by calling a QuPath save command.

```groovy
package com.yourorganization.qupath.agents

import qupath.lib.objects.PathAnnotationObject
import qupath.lib.roi.ROIs
import qupath.lib.gui.scripting.QP
import qupath.lib.gui.scripting.QPEx
import org.slf4j.LoggerFactory

class AnnotationAgent {
    String name = 'AnnotationAgent'
    Map<String, Object> config
    def logger

    void initialize(Map<String, Object> config) {
        this.config = config
        logger = LoggerFactory.getLogger(AnnotationAgent)
        logger.info("[${name}] Initializing annotation tasks.")
    }

    void execute() {
        def project = QP.getProject()
        project.getImageList().each { entry ->
            def imageData = entry.readImageData()
            QPEx.selectImageData(imageData)
            logger.info("[${name}] Annotating image: ${entry.getImageName()}")

            // Example: Pixel Classification to detect tissue
            QP.runCommand("Pixel Classifier", "Detect tissue", ["threshold": config.tissueThreshold])

            // Example: Create a rectangular ROI annotation
            def roi = ROIs.createRectangleROI(10, 10, 1000, 1000, ImagePlane.getDefaultPlane())
            def annotation = new PathAnnotationObject(roi)
            QP.getHierarchy().addPathObject(annotation)

            // Save the project to persist annotations
            QP.runCommand("Save project", "")
            logger.info("[${name}] Saved annotations for image: ${entry.getImageName()}")
        }
    }

    void cleanup() {
        logger.info("[${name}] AnnotationAgent cleanup complete.")
    }

    String getName() { return name }
}
```

### 4. TilingAgent

**Purpose**: Divide whole-slide images into smaller tiles for downstream processing, such as model inference or QC.

**Responsibilities**:

* For each image in the project, obtain its `ImageServer`.
* Create `RegionRequest` instances that define tile coordinates and size.
* Read each region from the server, save it as a PNG (or other format) to the specified output directory.
* Optionally filter tiles by tissue content if a threshold is provided.

```groovy
package com.yourorganization.qupath.agents

import qupath.lib.images.servers.ServerTools
import qupath.lib.images.writers.ImageWriterTools
import qupath.lib.regions.RegionRequest
import qupath.lib.gui.scripting.QPEx
import org.slf4j.LoggerFactory

class TilingAgent {
    String name = 'TilingAgent'
    Map<String, Object> config
    def logger

    void initialize(Map<String, Object> config) {
        this.config = config
        logger = LoggerFactory.getLogger(TilingAgent)
        logger.info("[${name}] Initializing tiling: tileSize=${config.tileSize}, overlap=${config.overlap}")
    }

    void execute() {
        def project = QPEx.getProject()
        project.getImageList().each { entry ->
            def server = ServerTools.getSupportedServer(Paths.get(entry.getImageURI()))
            def imageData = server.readImageData()
            int width = imageData.getWidth()
            int height = imageData.getHeight()
            int tileSize = config.tileSize
            int overlap = config.overlap
            int step = tileSize - overlap
            def outputDir = new File(config.tileOutputDir, entry.getImageName())
            outputDir.mkdirs()

            (0..<(height) step step).each { y ->
                (0..<(width) step step).each { x ->
                    def request = RegionRequest.createInstance(server.getPath(), 1, x, y, tileSize, tileSize)
                    def tileImg = server.readRegion(request).getImage()
                    def tileFile = new File(outputDir, "${entry.getImageName()}_tile_${x}_${y}.png")
                    ImageWriterTools.writeBufferedImage(tileImg, tileFile)
                    logger.info("[${name}] Saved tile: ${tileFile.name}")
                }
            }
        }
    }

    void cleanup() {
        logger.info("[${name}] TilingAgent cleanup complete.")
    }

    String getName() { return name }
}
```

### 5. ModelInferenceAgent

**Purpose**: Run a pre-trained machine learning or deep learning model (e.g., ONNX, TensorFlow) on each tile to generate predictions.

**Responsibilities**:

* Load the model from disk (e.g., ONNX file) using the appropriate Java inference API.
* Preprocess each tile image into the model’s input tensor format.
* Execute `session.run(...)` to obtain output probabilities or feature maps.
* Save the results (e.g., probability values) alongside tile filenames for later aggregation.

```groovy
package com.yourorganization.qupath.agents

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import javax.imageio.ImageIO
import qupath.lib.gui.scripting.QPEx
import org.slf4j.LoggerFactory

class ModelInferenceAgent {
    String name = 'ModelInferenceAgent'
    Map<String, Object> config
    OrtSession session
    def logger

    void initialize(Map<String, Object> config) {
        this.config = config
        logger = LoggerFactory.getLogger(ModelInferenceAgent)
        logger.info("[${name}] Loading ONNX model: ${config.modelPath}")
        def env = OrtEnvironment.getEnvironment()
        session = env.createSession(config.modelPath, new OrtSession.SessionOptions())
        logger.info("[${name}] Model loaded into memory.")
    }

    void execute() {
        new File(config.tileOutputDir).eachDir { imageDir ->
            imageDir.eachFileMatch(~/(?i).*\.png/) { File tileFile ->
                BufferedImage img = ImageIO.read(tileFile)
                // Preprocess: convert BufferedImage to float[][][][] as required
                float[][][][] inputTensor = ImageUtils.convertImageToTensor(img)
                def tensor = OnnxTensor.createTensor(session.getEnvironment(), inputTensor)
                def results = session.run(Collections.singletonMap(config.inputName, tensor))
                float[][] outputProbs = results.get(0).getValue() as float[][]
                float tumorProb = outputProbs[0][config.tumorClassIndex]
                def outFile = new File(config.resultOutputDir, tileFile.name.replace('.png', '_prob.txt'))
                outFile.text = tumorProb.toString()
                logger.info("[${name}] Saved probability ${tumorProb} for ${tileFile.name}")
                tensor.close()
                results.close()
            }
        }
    }

    void cleanup() {
        session.close()
        logger.info("[${name}] ModelInferenceAgent cleanup complete.")
    }

    String getName() { return name }
}
```

> **Helper Note**: You must implement `ImageUtils.convertImageToTensor(BufferedImage)` to normalize pixel values (e.g., divide by 255), reorder to CHW format, and ensure the tensor matches the model’s expected input shape.

### 6. PostProcessingAgent

**Purpose**: Aggregate inference results (e.g., per-tile probabilities) into slide-level outputs such as heatmaps or annotations.

**Responsibilities**:

* Read per-tile probability files (`*_prob.txt`) from the inference step.
* Parse tile coordinates embedded in filenames to reconstruct their positions.
* Create a heatmap image, or generate binary detections for regions exceeding a threshold.
* Add detections back into QuPath (e.g., as `PathDetectionObject`s) or save heatmap files to disk.

```groovy
package com.yourorganization.qupath.agents

import qupath.lib.objects.PathDetectionObject
import qupath.lib.roi.ROIs
import qupath.lib.gui.scripting.QPEx
import org.slf4j.LoggerFactory

class PostProcessingAgent {
    String name = 'PostProcessingAgent'
    Map<String, Object> config
    def logger

    void initialize(Map<String, Object> config) {
        this.config = config
        logger = LoggerFactory.getLogger(PostProcessingAgent)
        logger.info("[${name}] Initializing post-processing (threshold=${config.threshold})")
    }

    void execute() {
        new File(config.resultOutputDir).eachFile { File probFile ->
            if (!probFile.name.endsWith('_prob.txt')) return
            float prob = probFile.text.toFloat()
            if (prob >= config.threshold) {
                // Extract tile coordinates from filename: basename_tile_x_y_prob.txt
                def base = probFile.name.replace('_prob.txt', '')
                def parts = base.split('_tile_')
                def coords = parts[1].split('_')
                int x = coords[0].toInteger()
                int y = coords[1].toInteger()
                int tileSize = config.tileSize
                def centerX = x + tileSize / 2
                def centerY = y + tileSize / 2
                // Create a circular ROI at the tile center
                def roi = ROIs.createEllipseROI(centerX, centerY, tileSize / 2, tileSize / 2, ImagePlane.getDefaultPlane())
                def detection = new PathDetectionObject(roi, prob)
                QPEx.getCurrentHierarchy().addPathObject(detection)
                logger.info("[${name}] Added detection at (${centerX}, ${centerY}) with prob=${prob}")
            }
        }
        QPEx.saveProject()
    }

    void cleanup() {
        logger.info("[${name}] PostProcessingAgent cleanup complete.")
    }

    String getName() { return name }
}
```

### 7. ExportAgent

**Purpose**: Export annotations and results in standard formats (GeoJSON, CSV, PDF/HTML reports).

**Responsibilities**:

* Traverse annotation/detection hierarchies to collect `PathAnnotationObject` and `PathDetectionObject` instances.
* Convert ROIs to GeoJSON geometry and write to a file.
* Summarize inference results (mean, max probabilities per slide) into a CSV.
* Optionally produce an HTML or PDF report that embeds images and tables.

```groovy
package com.yourorganization.qupath.agents

import groovy.json.JsonBuilder
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.gui.scripting.QPEx
import qupath.lib.projects.QuPathProject
import org.slf4j.LoggerFactory

class ExportAgent {
    String name = 'ExportAgent'
    Map<String, Object> config
    def logger

    void initialize(Map<String, Object> config) {
        this.config = config
        logger = LoggerFactory.getLogger(ExportAgent)
        logger.info("[${name}] Preparing to export results.")
    }

    void execute() {
        // Export annotations to GeoJSON
        def project = QuPathProject.openProject(config.projectPath)
        def features = []
        project.getImageList().each { entry ->
            def imageData = entry.readImageData()
            def hierarchy = imageData.getHierarchy()
            hierarchy.getFlattenedObjectList(null).findAll { it instanceof PathAnnotationObject }.each { ann ->
                def coords = ann.getROI().getPolygon().points.collect { [it.X, it.Y] }
                features << [
                    type: 'Feature',
                    geometry: [type: 'Polygon', coordinates: [coords]],
                    properties: [image: entry.getImageName(), annotation: ann.getName()]
                ]
            }
        }
        def geojson = [type: 'FeatureCollection', features: features]
        new File(config.geojsonOutput).text = JsonBuilder(geojson).toPrettyString()
        logger.info("[${name}] GeoJSON exported: ${config.geojsonOutput}")

        // Export summary CSV
        def csvFile = new File(config.csvOutput)
        csvFile.withWriter { writer ->
            writer.writeLine("SlideName,MeanProbability,MaxProbability")
            config.probSummary.each { summary ->
                writer.writeLine("${summary.slide},${summary.mean},${summary.max}")
            }
        }
        logger.info("[${name}] CSV summary exported: ${config.csvOutput}")
    }

    void cleanup() {
        logger.info("[${name}] ExportAgent cleanup complete.")
    }

    String getName() { return name }
}
```

### 8. QC\_Agent

**Purpose**: Perform quality control checks on tile and inference outputs to identify missing or out-of-range results.

**Responsibilities**:

* Verify that each tile has a corresponding probability file.
* Check that probability values are in the \[0, 1] range.
* Log any missing or invalid files.
* Write a QC report (CSV or TXT) summarizing issues.

```groovy
package com.yourorganization.qupath.agents

import org.slf4j.LoggerFactory

class QC_Agent {
    String name = 'QC_Agent'
    Map<String, Object> config
    def logger
    List<String> qcIssues = []

    void initialize(Map<String, Object> config) {
        this.config = config
        logger = LoggerFactory.getLogger(QC_Agent)
        logger.info("[${name}] Starting QC checks.")
    }

    void execute() {
        def tileDir = new File(config.tileOutputDir)
        def resultDir = new File(config.resultOutputDir)

        tileDir.eachDir { slideDir ->
            def tileFiles = slideDir.listFiles().findAll { it.name.endsWith('.png') }
            tileFiles.each { tileFile ->
                def probFile = new File(resultDir, tileFile.name.replace('.png', '_prob.txt'))
                if (!probFile.exists()) {
                    qcIssues << "Missing probability for tile: ${tileFile.name}"
                } else {
                    float prob = probFile.text.toFloat()
                    if (prob < 0f || prob > 1f) {
                        qcIssues << "Invalid probability in ${probFile.name}: ${prob}"
                    }
                }
            }
        }

        // Save QC report
        def reportFile = new File(config.qcReportPath)
        reportFile.withWriter { writer ->
            writer.writeLine("QC Report - ${new Date().format('yyyy-MM-dd HH:mm:ss')}")
            if (qcIssues.isEmpty()) {
                writer.writeLine("No QC issues detected.")
            } else {
                qcIssues.each { writer.writeLine(it) }
            }
        }
        logger.info("[${name}] QC report saved: ${config.qcReportPath}")
    }

    void cleanup() {
        logger.info("[${name}] QC_Agent cleanup complete.")
    }

    String getName() { return name }
}
```

### 9. ReportingAgent

**Purpose**: Compile final metrics, embed representative images (e.g., heatmaps), and generate a human-readable report (PDF or HTML).

**Responsibilities**:

* Aggregate pipeline statistics (number of slides processed, annotations, QC issues, etc.).
* Embed images or tables into a PDF document.
* Save the report to disk.

```groovy
package com.yourorganization.qupath.agents

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.slf4j.LoggerFactory

class ReportingAgent {
    String name = 'ReportingAgent'
    Map<String, Object> config
    def logger

    void initialize(Map<String, Object> config) {
        this.config = config
        logger = LoggerFactory.getLogger(ReportingAgent)
        logger.info("[${name}] Initializing report generation.")
    }

    void execute() {
        PDDocument document = new PDDocument()
        PDPage page = new PDPage()
        document.addPage(page)
        PDPageContentStream contentStream = new PDPageContentStream(document, page)

        contentStream.beginText()
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18)
        contentStream.newLineAtOffset(50, 750)
        contentStream.showText("Digital Pathology Pipeline Report")
        contentStream.newLineAtOffset(0, -30)
        contentStream.setFont(PDType1Font.HELVETICA, 12)
        contentStream.showText("Date: ${new Date().format('yyyy-MM-dd')}")
        contentStream.newLineAtOffset(0, -20)
        contentStream.showText("Total Slides Processed: ${config.totalSlides}")
        contentStream.newLineAtOffset(0, -20)
        contentStream.showText("Total Annotations Created: ${config.totalAnnotations}")
        contentStream.newLineAtOffset(0, -20)
        contentStream.showText("QC Issues Found: ${config.qcIssuesCount}")
        contentStream.endText()

        // Optionally embed images or tables here

        contentStream.close()
        document.save(config.reportPath)
        document.close()
        logger.info("[${name}] Report generated: ${config.reportPath}")
    }

    void cleanup() {
        logger.info("[${name}] ReportingAgent cleanup complete.")
    }

    String getName() { return name }
}
```

---

## Creating Custom Agents

While the standard agents cover many workflows, you might need to implement custom agents for specialized tasks (e.g., stain normalization, custom segmentation). Below are guidelines for creating and integrating custom agents.

### Defining a New Agent

1. **Create a Groovy File**: In `agents/`, create `<YourAgentName>Agent.groovy`.
2. **Package and Imports**:

   ```groovy
   package com.yourorganization.qupath.agents

   import qupath.lib.objects.PathObject
   import qupath.lib.gui.scripting.QP
   import org.slf4j.LoggerFactory
   ```
3. **Implement the Agent Class** with the standard methods:

   ```groovy
   class YourAgentNameAgent {
       String name = 'YourAgentNameAgent'
       Map<String, Object> config
       def logger

       void initialize(Map<String, Object> config) {
           this.config = config
           logger = LoggerFactory.getLogger(YourAgentNameAgent)
           logger.info("[${name}] Initializing...")
           validateConfig(['param1', 'param2'])
       }

       void execute() {
           // Core logic here
       }

       void cleanup() {
           logger.info("[${name}] Cleanup complete.")
       }

       String getName() { return name }

       void validateConfig(List<String> requiredKeys) {
           requiredKeys.each { key ->
               if (!config.containsKey(key)) {
                   throw new IllegalArgumentException("Missing configuration key: ${key}")
               }
           }
       }
   }
   ```
4. **Document Expected Parameters**: Clearly describe the required configuration keys in comments or a README.
5. **Ensure Idempotency**: Agents should be safe to rerun without causing inconsistent state (e.g., check for existing outputs before writing new ones).

### Agent Interface and Base Class

To promote consistency, consider creating an abstract base class:

```groovy
package com.yourorganization.qupath.agents

abstract class BaseAgent {
    String name = this.getClass().getSimpleName()
    Map<String, Object> config
    def logger

    abstract void initialize(Map<String, Object> config)
    abstract void execute()
    abstract void cleanup()

    String getName() { return name }

    void validateConfig(List<String> requiredKeys) {
        requiredKeys.each { key ->
            if (!config.containsKey(key)) {
                throw new IllegalArgumentException("Missing configuration key: ${key}")
            }
        }
    }
}
```

Each custom agent can extend `BaseAgent` and inherit validation logic or logging setup. Example:

```groovy
class CustomAgent extends BaseAgent {
    void initialize(Map<String, Object> config) {
        this.config = config
        logger = LoggerFactory.getLogger(CustomAgent)
        validateConfig(['inputPath', 'outputPath'])
        logger.info("[${name}] Initialized with input=${config.inputPath}")
    }

    void execute() {
        // Agent-specific logic here
    }

    void cleanup() {
        logger.info("[${name}] Cleanup complete.")
    }
}
```

### Registering Agents in the Pipeline

An orchestration script (e.g., `runPipeline.groovy`) controls the execution order:

```groovy
import groovy.yaml.YamlSlurper
import com.yourorganization.qupath.agents.*

// Load the pipeline configuration
def pipelineConfig = new YamlSlurper().parse(new File('configs/pipeline.yaml'))

// Instantiate agents in the desired sequence
List<BaseAgent> agents = [
    new ProjectManagerAgent(),
    new ImageImportAgent(),
    new AnnotationAgent(),
    new TilingAgent(),
    new ModelInferenceAgent(),
    new PostProcessingAgent(),
    new QC_Agent(),
    new ExportAgent(),
    new ReportingAgent()
]

try {
    // Initialize all agents with their respective config sections
    agents.each { agent -> agent.initialize(pipelineConfig[agent.getName()]) }

    // Execute agents sequentially
    agents.each { agent ->
        println "[Pipeline] Running ${agent.getName()}"
        agent.execute()
    }
} catch (Exception e) {
    println "[Pipeline] Error encountered: ${e.message}"; e.printStackTrace()
} finally {
    // Perform cleanup for all agents
    agents.each { agent -> agent.cleanup() }
    println "[Pipeline] Execution complete."
}
```

The corresponding `pipeline.yaml` structure:

```yaml
ProjectManagerAgent:
  outputDirs:
    - "outputs/annotations"
    - "outputs/tiles"
    - "outputs/results"
    - "outputs/reports"
  logPath: "logs/pipeline.log"
  projectPath: "project/qupath.qpj"

ImageImportAgent:
  imageDirectory: "data/images"

AnnotationAgent:
  tissueThreshold: 0.15

TilingAgent:
  tileSize: 512
  overlap: 64
  tileOutputDir: "outputs/tiles"

ModelInferenceAgent:
  modelPath: "models/TumorClassifier.onnx"
  inputName: "input_0"
  tumorClassIndex: 1
  tileOutputDir: "outputs/tiles"
  resultOutputDir: "outputs/results"

PostProcessingAgent:
  tileOutputDir: "outputs/tiles"
  heatmapOutputDir: "outputs/results/heatmaps"
  threshold: 0.5
  tileSize: 512

QC_Agent:
  tileOutputDir: "outputs/tiles"
  resultOutputDir: "outputs/results"
  qcReportPath: "outputs/reports/qc_report.txt"

ExportAgent:
  projectPath: "project/qupath.qpj"
  geojsonOutput: "outputs/annotations/annotations.geojson"
  csvOutput: "outputs/results/summary.csv"
  probSummary:
    - slide: "slide1"
      mean: 0.60
      max: 0.95

ReportingAgent:
  totalSlides: 10
  totalAnnotations: 200
  qcIssuesCount: 5
  reportPath: "outputs/reports/final_report.pdf"
```

---

## Agent Lifecycle and Execution Flow

A well-designed agent follows a clear lifecycle:

1. **Initialization**:

   * Load and validate configuration.
   * Create output directories if they do not exist.
   * Open or create the QuPath project via `QPEx`.
   * Load resources (e.g., models) into memory.
   * Initialize loggers.

2. **Execution**:

   * Perform the agent’s primary function (e.g., import images, detect ROIs, run inference).
   * Handle non-fatal exceptions internally and log warnings or errors.

3. **Cleanup**:

   * Release resources (e.g., close ONNX sessions, file handles).
   * Save the project state with `QPEx.saveProject()`.
   * Finalize or flush log output.

### Initialization

* Validate that required configuration keys are present.
* Create missing directories (e.g., `outputs/tiles`, `outputs/results`).
* Open an existing QuPath project or create a new one.
* Load pre-trained models or other large resources once, to avoid repeated I/O.

### Execution

* Process images or tiles one at a time to limit memory usage.
* Use QuPath APIs (`QP.runCommand()`, `ServerTools.getSupportedServer()`) for built-in tasks and low-level access.
* Ensure intermediate outputs are saved to disk to allow reproducibility and recovery.

### Error Handling

* Catch and log non-critical exceptions within `execute()` so subsequent images/tiles can still be processed.
* For critical errors (e.g., missing configuration, failed model load), rethrow or abort to stop the pipeline and alert the user.
* Provide clear, descriptive log messages that identify the agent and the error context.

### Cleanup

* Close any open model sessions (e.g., `session.close()` for ONNX).
* Save the QuPath project to persist annotations and modifications.
* Flush or close any open log appenders.

---

## Configuration and Parameters

### YAML/JSON Config Files

Centralizing pipeline parameters in a YAML or JSON file makes it easy to:

* Adjust settings without modifying code.
* Keep track of parameter changes via version control.
* Reproduce previous runs by reusing an older config file.

The config file structure should mirror agent names:

```yaml
<AgentName>:
  param1: value1
  param2: value2
  ...
```

Each agent reads its own subsection (e.g., `pipelineConfig[agent.getName()]`) in `runPipeline.groovy`.

### Runtime Arguments

You can override certain configuration values at runtime using command-line flags. In `runPipeline.groovy`, add a `CliBuilder` parser:

```groovy
import groovy.cli.picocli.CliBuilder

def cli = new CliBuilder(usage: 'groovy runPipeline.groovy [options]')
cli.h(longOpt: 'help', 'Display usage information')
cli.c(longOpt: 'config', args: 1, argName: 'configFile', 'Path to pipeline configuration file')
cli.d(longOpt: 'dry-run', 'Perform a dry run without writing outputs')
def options = cli.parse(args)
if (options.h) {
    cli.usage()
    System.exit(0)
}

def configFilePath = options.c ?: 'configs/pipeline.yaml'
def pipelineConfig = new YamlSlurper().parse(new File(configFilePath))

// Agents can check pipelineConfig[agent.getName()].dryRun, etc.
```

This pattern allows flags such as `--config custom.yaml` or `--dry-run` to modify agent behavior.

---

## Best Practices

### Modularity

* Assign each agent a single responsibility (e.g., importing, tiling, inference).
* Extract shared utilities (image-to-tensor conversion, file helpers) into a common `utils.groovy` file.
* Avoid tight coupling: communicate between agents via filesystem outputs or a shared context object.

### Logging

* Use a consistent logging framework (e.g., SLF4J with Logback) across all agents.
* Prefix log messages with agent names and timestamps for clarity.
* Configure logback.xml for rotating file logs if pipeline runs generate large volumes of logs.

### Performance Considerations

* **Batch Processing**: Process images or tiles one at a time instead of loading entire WSIs into memory.
* **Parallelization**: For time-consuming tasks (tiling, inference), consider parallel execution (e.g., Groovy’s GPars, Java’s ExecutorService) while monitoring memory usage.
* **Caching**: If multiple agents use the same intermediate (e.g., tissue masks), cache those results on disk to avoid repeated computation.

### Version Control

* Track both code and configuration files in Git.
* Tag stable pipeline releases (e.g., `v1.0`, `v1.1`) to freeze a known-good configuration.
* Use feature branches for experimenting with new agents or altering pipeline structure.

---

## Examples and Use Cases

Below are three example pipelines demonstrating common workflows. These are meant as templates that you can adapt.

### Example 1: Batch Annotation Workflow

**Objective**: Automatically detect tissue regions across a batch of WSIs and export annotations.

**Agents Sequence**:

1. **ProjectManagerAgent**: Set up directories and open/create the QuPath project.
2. **ImageImportAgent**: Load all `.svs` files from `data/images/` into the project.
3. **AnnotationAgent**: Run a pixel classifier to detect tissue and create annotations.
4. **ExportAgent**: Export annotations to GeoJSON and CSV.

**Pipeline Configuration (`pipeline.yaml`)**:

```yaml
ProjectManagerAgent:
  outputDirs:
    - "outputs/annotations"
    - "outputs/tiles"
    - "outputs/results"
    - "outputs/reports"
  logPath: "logs/pipeline.log"
  projectPath: "project/qupath.qpj"

ImageImportAgent:
  imageDirectory: "data/images"

AnnotationAgent:
  tissueThreshold: 0.10  # Threshold for pixel classifier

ExportAgent:
  projectPath: "project/qupath.qpj"
  geojsonOutput: "outputs/annotations/tissue_annotations.geojson"
  csvOutput: "outputs/annotations/tissue_summary.csv"
  probSummary: []    
```

**Orchestration Script (`runPipeline.groovy`)**:

```groovy
import groovy.yaml.YamlSlurper
import com.yourorganization.qupath.agents.*

// Load configuration
def pipelineConfig = new YamlSlurper().parse(new File('configs/pipeline.yaml'))

// Instantiate agents in order
List<BaseAgent> agents = [
    new ProjectManagerAgent(),
    new ImageImportAgent(),
    new AnnotationAgent(),
    new ExportAgent()
]

// Execute pipeline
agents.each { it.initialize(pipelineConfig[it.getName()]) }
agents.each { it.execute() }
agents.each { it.cleanup() }

println "Batch annotation workflow complete."  
```

### Example 2: Tile-Based Model Inference

**Objective**: Tile WSIs, run a tumor classification model on each tile, and generate a heatmap overlay.

**Agents Sequence**:

1. **ProjectManagerAgent**
2. **ImageImportAgent**
3. **TilingAgent**: Create 500 × 500 tiles with 100 px overlap, filtering out low-tissue tiles.
4. **ModelInferenceAgent**: Apply a pre-trained ONNX tumor classifier to each tile.
5. **PostProcessingAgent**: Generate a heatmap from tile probabilities and annotate high-probability regions.
6. **QC\_Agent**: Check for missing or invalid probability files.
7. **ExportAgent**: Export tumor regions as GeoJSON and a summary CSV.
8. **ReportingAgent**: Produce a final PDF report.

**Pipeline Configuration (`pipeline.yaml`)**:

```yaml
ProjectManagerAgent:
  outputDirs:
    - "outputs/annotations"
    - "outputs/tiles"
    - "outputs/results"
    - "outputs/reports"
  logPath: "logs/pipeline.log"
  projectPath: "project/qupath.qpj"

ImageImportAgent:
  imageDirectory: "data/slides"

TilingAgent:
  tileSize: 500
  overlap: 100
  tileOutputDir: "outputs/tiles"
  tissueThreshold: 0.25

ModelInferenceAgent:
  modelPath: "models/TumorClassifier.onnx"
  inputName: "input_0"
  tumorClassIndex: 1
  tileOutputDir: "outputs/tiles"
  resultOutputDir: "outputs/results"

PostProcessingAgent:
  tileOutputDir: "outputs/tiles"
  heatmapOutputDir: "outputs/results/heatmaps"
  threshold: 0.70
  tileSize: 500

QC_Agent:
  tileOutputDir: "outputs/tiles"
  resultOutputDir: "outputs/results"
  qcReportPath: "outputs/reports/qc_report.txt"

ExportAgent:
  projectPath: "project/qupath.qpj"
  geojsonOutput: "outputs/annotations/tumor_regions.geojson"
  csvOutput: "outputs/results/tumor_summary.csv"
  probSummary:
    - slide: "slide1"
      mean: 0.55
      max: 0.95

ReportingAgent:
  totalSlides: 1
  totalAnnotations: 25
  qcIssuesCount: 0
  reportPath: "outputs/reports/tumor_report.pdf"
```

**Orchestration Script (`runPipeline.groovy`)**:

```groovy
import groovy.yaml.YamlSlurper
import com.yourorganization.qupath.agents.*

def pipelineConfig = new YamlSlurper().parse(new File('configs/pipeline.yaml'))
List<BaseAgent> agents = [
    new ProjectManagerAgent(),
    new ImageImportAgent(),
    new TilingAgent(),
    new ModelInferenceAgent(),
    new PostProcessingAgent(),
    new QC_Agent(),
    new ExportAgent(),
    new ReportingAgent()
]

agents.each { it.initialize(pipelineConfig[it.getName()]) }
agents.each { it.execute() }
agents.each { it.cleanup() }

println "Tile-based model inference pipeline complete."  
```

### Example 3: Quality Control Dashboard Export

**Objective**: Identify tiles with missing or invalid probability values and generate a QC report.

**Agents Sequence**:

1. **QC\_Agent**: Scan tile and result directories to log missing or out-of-range probabilities.
2. **ExportAgent**: Export QC issues to a CSV file.
3. **ReportingAgent**: Create a PDF dashboard embedding the QC CSV table.

**Pipeline Configuration (`pipeline.yaml`)**:

```yaml
QC_Agent:
  tileOutputDir: "outputs/tiles"
  resultOutputDir: "outputs/results"
  qcReportPath: "outputs/reports/qc_issues.csv"

ExportAgent:
  projectPath: "project/qupath.qpj"
  geojsonOutput: ""
  csvOutput: "outputs/reports/qc_issues.csv"
  probSummary: []

ReportingAgent:
  totalSlides: 1
  totalAnnotations: 0
  qcIssuesCount: 10
  reportPath: "outputs/reports/qc_dashboard.pdf"
```

**Orchestration Script (`runPipeline.groovy`)**:

```groovy
import groovy.yaml.YamlSlurper
import com.yourorganization.qupath.agents.*

def pipelineConfig = new YamlSlurper().parse(new File('configs/pipeline.yaml'))
List<BaseAgent> agents = [new QC_Agent(), new ExportAgent(), new ReportingAgent()]

agents.each { it.initialize(pipelineConfig[it.getName()]) }
agents.each { it.execute() }
agents.each { it.cleanup() }

println "QC dashboard pipeline complete."  
```

---

## Troubleshooting and FAQs

### Common Errors

1. **Missing Configuration Key**:

   * **Symptom**: `IllegalArgumentException: Missing configuration key: <key>`
   * **Solution**: Ensure the key appears under the correct agent name in `pipeline.yaml`.

2. **Unsupported Image Format**:

   * **Symptom**: `ServerTools.getSupportedServer()` returns `null`.
   * **Solution**: Convert images to supported formats (e.g., `.svs`, `.tiff`).

3. **Model Loading Failure**:

   * **Symptom**: `OrtException: Failed to load model` or `FileNotFoundException`.
   * **Solution**: Verify the `modelPath` is correct and the ONNX file is compatible with the runtime.

4. **Tile Out-of-Bounds**:

   * **Symptom**: `ArrayIndexOutOfBoundsException` near image edges when tiling.
   * **Solution**: In `TilingAgent`, ensure you use `Math.min(x + tileSize, width)` and `Math.min(y + tileSize, height)` for final tiles.

5. **Annotation Not Saved**:

   * **Symptom**: Annotations do not appear in the project after running `AnnotationAgent`.
   * **Solution**: Call `QPEx.saveProject()` after adding annotations and reload the project in QuPath.

6. **Permission Denied**:

   * **Symptom**: `AccessDeniedException` when writing files to `outputs/`.
   * **Solution**: Verify directory permissions and ensure QuPath or the script has write access.

### Debugging Tips

* **Enable Verbose Logging**: Temporarily set the log level to DEBUG in `logback.xml` or use `logger.debug()` statements.
* **Agent Isolation**: Run individual agent scripts in QuPath’s Script Editor to validate họ comportamento độc lập before integrating them.
* **Inspect Intermediate Outputs**: Manually check a handful of tiles or probability files to confirm expected output.
* **Use QuPath GUI**: For quick tests, run QuPath commands interactively (e.g., via QPEx) to ensure correct functionality.
* **Unit Tests**: Write JUnit or Spock tests for utility methods (e.g., converting images to tensors).

---

## References and Further Reading

1. **QuPath Scripting Documentation**:

   * QP API: cite[https://qupath.github.io/javadoc/docs/qupath/lib/scripting/QP.html](https://qupath.github.io/javadoc/docs/qupath/lib/scripting/QP.html)
   * QPEx API: cite[https://qupath.github.io/javadoc/docs/qupath/lib/gui/scripting/QPEx.html](https://qupath.github.io/javadoc/docs/qupath/lib/gui/scripting/QPEx.html)

2. **QuPath Command Line**:

   * CLI Guide: cite[https://qupath.readthedocs.io/en/stable/docs/advanced/command\_line.html](https://qupath.readthedocs.io/en/stable/docs/advanced/command_line.html)

3. **QuPath Gradle Scripting Project**:

   * GitHub Repository: cite[https://github.com/qupath/qupath-gradle-scripting-project](https://github.com/qupath/qupath-gradle-scripting-project)

4. **Groovy Language Documentation**:

   * Official Site: [https://groovy-lang.org/](https://groovy-lang.org/)

5. **ONNX Runtime for Java**:

   * API Documentation: [https://onnxruntime.ai/docs/api/java/](https://onnxruntime.ai/docs/api/java/)

6. **Apache PDFBox**:

   * Official Website: [https://pdfbox.apache.org/](https://pdfbox.apache.org/)

7. **Image Scientist Resources**:

   * Image Analysis Tutorials: cite[https://www.imagescientist.com/image-analysis](https://www.imagescientist.com/image-analysis)

8. **QuPath Javadoc Index**:

   * Comprehensive Index: cite[https://qupath.github.io/javadoc/docs/index-all.html](https://qupath.github.io/javadoc/docs/index-all.html)

9. **QuPath Workshop Examples (i2k2024)**:

   * GitHub: cite[https://github.com/qupath/i2k2024/tree/main/workshops/groovy-scripters](https://github.com/qupath/i2k2024/tree/main/workshops/groovy-scripters)

10. **Community Forums**:

    * QuPath Forum: [https://forum.image.sc/c/qupath/](https://forum.image.sc/c/qupath/)
    * Stack Overflow (tag: `qupath`)

---

*End of AGENTS.md*
