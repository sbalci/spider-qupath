// SPIDER_Plugin_Menu.groovy
// Enables SPIDER digital pathology analysis integration within QuPath

// Add required imports at the top
import qupath.lib.objects.PathObjects
import qupath.lib.regions.ImagePlane
import qupath.lib.roi.ROIs
import qupath.lib.objects.classes.PathClass

import javafx.application.Platform
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.geometry.Insets
import javafx.stage.Stage
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.scene.input.KeyCombination
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.io.GsonTools
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import java.nio.file.Paths

// Configuration class to store user preferences
class SPIDERConfig {
    static String pythonPath = ""
    static String modelsBasePath = ""
    static String lastSelectedModel = "colorectal"
    static boolean showTutorial = true
    
    static void save() {
        // Create config directory if needed
        def configDir = buildFilePath(PROJECT_BASE_DIR, "config")
        new File(configDir).mkdirs()
        
        def configFile = new File(buildFilePath(configDir, "spider_config.json"))
        def config = [
            pythonPath: pythonPath,
            modelsBasePath: modelsBasePath,
            lastSelectedModel: lastSelectedModel,
            showTutorial: showTutorial
        ]
        
        def gson = GsonTools.getInstance(true)
        configFile.text = gson.toJson(config)
        println("Saved configuration to: ${configFile.getAbsolutePath()}")
    }
    
    static void load() {
        def configFile = new File(buildFilePath(PROJECT_BASE_DIR, "config", "spider_config.json"))
        if (configFile.exists()) {
            try {
                // Use Gson instead of JsonSlurper
                def jsonParser = new JsonParser()
                def jsonObj = jsonParser.parse(configFile.text).getAsJsonObject()
                
                pythonPath = jsonObj.has("pythonPath") ? jsonObj.get("pythonPath").getAsString() : ""
                modelsBasePath = jsonObj.has("modelsBasePath") ? jsonObj.get("modelsBasePath").getAsString() : ""
                lastSelectedModel = jsonObj.has("lastSelectedModel") ? jsonObj.get("lastSelectedModel").getAsString() : "colorectal"
                showTutorial = jsonObj.has("showTutorial") ? jsonObj.get("showTutorial").getAsBoolean() : true
                
                println("Loaded configuration from: ${configFile.getAbsolutePath()}")
            } catch (Exception e) {
                println("Error loading configuration: ${e.getMessage()}")
                // Use defaults if loading fails
            }
        } else {
            println("No configuration file found at: ${configFile.getAbsolutePath()}")
        }
    }
}

// Load configuration on startup
SPIDERConfig.load()

// Model information
def getModelInfo() {
    return [
        colorectal: [
            name: "SPIDER Colorectal",
            fullName: "SPIDER-colorectal-model",
            description: "13 classes for colorectal tissue analysis",
            classes: ["Adenocarcinoma high grade", "Adenocarcinoma low grade", "Adenoma high grade", 
                      "Adenoma low grade", "Fat", "Hyperplastic polyp", "Inflammation", "Mucus", 
                      "Muscle", "Necrosis", "Sessile serrated lesion", "Stroma healthy", "Vessels"],
            accuracy: "91.4%"
        ],
        skin: [
            name: "SPIDER Skin",
            fullName: "SPIDER-skin-model",
            description: "24 classes for skin pathology analysis",
            classes: ["Basal Cell Carcinoma", "Melanoma invasive", "Melanoma in situ", 
                      "Squamous Cell Carcinoma", "Epidermis", "Vessels", "etc."],
            accuracy: "94.0%"
        ],
        thorax: [
            name: "SPIDER Thorax",
            fullName: "SPIDER-thorax-model",
            description: "14 classes for thoracic tissue analysis",
            classes: ["Small cell carcinoma", "Non-small cell carcinoma", "Alveoli", 
                      "Vessels", "Bronchus", "etc."],
            accuracy: "96.2%"
        ]
    ]
}

// Get class abbreviations for each model type
def getClassAbbreviations(String modelType) {
    def abbrevMap = [:]
    if (modelType == "colorectal") {
        abbrevMap = [
            "Adenocarcinoma high grade": "Adeno H",
            "Adenocarcinoma low grade": "Adeno L",
            "Adenoma high grade": "Aden H",
            "Adenoma low grade": "Aden L",
            "Fat": "Fat",
            "Hyperplastic polyp": "Hyper",
            "Inflammation": "Infl",
            "Mucus": "Mucus",
            "Muscle": "Musc",
            "Necrosis": "Necr",
            "Sessile serrated lesion": "SSL",
            "Stroma healthy": "Stroma",
            "Vessels": "Vessel"
        ]
    } else if (modelType == "skin") {
        abbrevMap = [
            "Basal Cell Carcinoma": "BCC",
            "Melanoma invasive": "Mel Inv",
            "Melanoma in situ": "Mel IS",
            "Squamous Cell Carcinoma": "SCC",
            "Epidermis": "Epid",
            "Dermis": "Derm",
            "Vessels": "Vessel",
            "Hair follicle": "Hair",
            "Sebaceous gland": "Seb",
            "Sweat gland": "Sweat"
        ]
    } else if (modelType == "thorax") {
        abbrevMap = [
            "Small cell carcinoma": "Small C",
            "Non-small cell carcinoma": "NSCLC",
            "Alveoli": "Alv",
            "Vessels": "Vessel",
            "Bronchus": "Bronch",
            "Pleura": "Pleur",
            "Lymphoid tissue": "Lymph"
        ]
    }
    return abbrevMap
}

// Create main menu
def createMainMenu() {
    Platform.runLater {
        def stage = new Stage()
        stage.setTitle("SPIDER Pathology Analysis")
        
        def mainPane = new VBox(15)
        mainPane.setPadding(new Insets(20))
        mainPane.setStyle("-fx-background-color: #f0f0f0;")
        
        // Header
        def headerLabel = new Label("SPIDER Digital Pathology Analysis")
        headerLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;")
        
        def subheaderLabel = new Label("AI-powered tissue classification for pathology")
        subheaderLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;")
        
        // Model selection
        def modelBox = new VBox(10)
        def modelLabel = new Label("Select Model:")
        modelLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;")
        
        def modelChoiceBox = new ChoiceBox<String>()
        modelChoiceBox.getItems().addAll("Colorectal", "Skin", "Thorax")
        modelChoiceBox.setValue(SPIDERConfig.lastSelectedModel.capitalize())
        modelChoiceBox.setStyle("-fx-pref-width: 300px;")
        
        def modelDescLabel = new Label()
        modelDescLabel.setWrapText(true)
        modelDescLabel.setStyle("-fx-text-fill: #555555;")
        
        // Update description when model changes
        def updateModelDesc = {
            def selected = modelChoiceBox.getValue().toLowerCase()
            def info = getModelInfo()[selected]
            modelDescLabel.setText("${info.description}\nAccuracy: ${info.accuracy}")
            SPIDERConfig.lastSelectedModel = selected
        }
        
        modelChoiceBox.setOnAction { updateModelDesc() }
        updateModelDesc()
        
        modelBox.getChildren().addAll(modelLabel, modelChoiceBox, modelDescLabel)
        
        // Workflow buttons
        def buttonBox = new VBox(10)
        buttonBox.setStyle("-fx-padding: 20 0 0 0;")
        
        def createStyledButton = { text, action ->
            def button = new Button(text)
            button.setStyle("""
                -fx-pref-width: 300px;
                -fx-pref-height: 40px;
                -fx-font-size: 14px;
                -fx-background-color: #3498db;
                -fx-text-fill: white;
                -fx-cursor: hand;
            """)
            button.setOnMouseEntered { 
                button.setStyle(button.getStyle() + "-fx-background-color: #2980b9;") 
            }
            button.setOnMouseExited { 
                button.setStyle(button.getStyle().replace("-fx-background-color: #2980b9;", "-fx-background-color: #3498db;"))
            }
            button.setOnAction(action)
            return button
        }
        
        // Main workflow buttons
        def classifyAnnotationsBtn = createStyledButton("1. Classify Selected Annotations") {
            stage.close()
            runClassifyAnnotations(modelChoiceBox.getValue().toLowerCase())
        }
        
        def tileAnalysisBtn = createStyledButton("2. Tile Analysis (Detailed)") {
            stage.close()
            runTileAnalysis(modelChoiceBox.getValue().toLowerCase())
        }
        
        def wholeSlideBtn = createStyledButton("3. Whole Slide Analysis") {
            stage.close()
            showWholeSlideDialog(modelChoiceBox.getValue().toLowerCase())
        }
        
        def tutorialBtn = createStyledButton("Show Tutorial") {
            showTutorial()
        }
        
        // Configuration button
        def configBtn = new Button("⚙ Configuration")
        configBtn.setStyle("""
            -fx-pref-width: 150px;
            -fx-background-color: #95a5a6;
            -fx-text-fill: white;
        """)
        configBtn.setOnAction {
            showConfigurationDialog()
        }
        
        buttonBox.getChildren().addAll(
            classifyAnnotationsBtn,
            tileAnalysisBtn,
            wholeSlideBtn,
            new Separator(),
            tutorialBtn,
            configBtn
        )
        
        // Help text
        def helpText = new TextArea()
        helpText.setText("""Quick Start Guide:
1. First, configure Python path and model locations
2. Draw annotations on your slide
3. Select annotations and choose classification method
4. View results with color-coded classifications""")
        helpText.setEditable(false)
        helpText.setWrapText(true)
        helpText.setPrefRowCount(4)
        helpText.setStyle("-fx-font-size: 12px;")
        
        // Layout
        mainPane.getChildren().addAll(
            headerLabel,
            subheaderLabel,
            new Separator(),
            modelBox,
            buttonBox,
            new Separator(),
            helpText
        )
        
        def scene = new javafx.scene.Scene(mainPane, 400, 600)
        stage.setScene(scene)
        stage.setResizable(false)
        stage.show()
        
        // Show tutorial on first run
        if (SPIDERConfig.showTutorial) {
            showTutorial()
            SPIDERConfig.showTutorial = false
            SPIDERConfig.save()
        }
    }
}

// Configuration dialog
def showConfigurationDialog() {
    Platform.runLater {
        def dialog = new Dialog<ButtonType>()
        dialog.setTitle("SPIDER Configuration")
        dialog.setHeaderText("Configure SPIDER Settings")
        
        def grid = new GridPane()
        grid.setHgap(10)
        grid.setVgap(10)
        grid.setPadding(new Insets(20))
        
        // Python path
        def pythonField = new TextField(SPIDERConfig.pythonPath)
        pythonField.setPrefWidth(300)
        def pythonBtn = new Button("Browse...")
        pythonBtn.setOnAction {
            def chooser = new FileChooser()
            chooser.setTitle("Select Python Executable")
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Python", "*.exe", "python")
            )
            def file = chooser.showOpenDialog(dialog.getOwner())
            if (file != null) {
                pythonField.setText(file.getAbsolutePath())
            }
        }
        
        // Models base path
        def modelsField = new TextField(SPIDERConfig.modelsBasePath)
        modelsField.setPrefWidth(300)
        def modelsBtn = new Button("Browse...")
        modelsBtn.setOnAction {
            def chooser = new DirectoryChooser()
            chooser.setTitle("Select Models Directory")
            def dir = chooser.showDialog(dialog.getOwner())
            if (dir != null) {
                modelsField.setText(dir.getAbsolutePath())
            }
        }
        
        grid.add(new Label("Python Path:"), 0, 0)
        grid.add(pythonField, 1, 0)
        grid.add(pythonBtn, 2, 0)
        
        grid.add(new Label("Models Directory:"), 0, 1)
        grid.add(modelsField, 1, 1)
        grid.add(modelsBtn, 2, 1)
        
        // Test button
        def testBtn = new Button("Test Configuration")
        testBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;")
        testBtn.setOnAction {
            testConfiguration(pythonField.getText(), modelsField.getText())
        }
        grid.add(testBtn, 1, 2)
        
        dialog.getDialogPane().setContent(grid)
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)
        
        def result = dialog.showAndWait()
        if (result.isPresent() && result.get() == ButtonType.OK) {
            SPIDERConfig.pythonPath = pythonField.getText()
            SPIDERConfig.modelsBasePath = modelsField.getText()
            SPIDERConfig.save()
            Dialogs.showInfoNotification("Configuration Saved", "Settings have been saved successfully")
        }
    }
}

// Test configuration
def testConfiguration(pythonPath, modelsPath) {
    // Test Python
    def pythonOk = new File(pythonPath).exists()
    
    // Test models
    def colorectalOk = new File(modelsPath, "SPIDER-colorectal-model").exists()
    def skinOk = new File(modelsPath, "SPIDER-skin-model").exists()
    def thoraxOk = new File(modelsPath, "SPIDER-thorax-model").exists()
    
    def message = "Configuration Test Results:\n\n"
    message += "Python: ${pythonOk ? '✓ Found' : '✗ Not Found'}\n"
    message += "Colorectal Model: ${colorectalOk ? '✓ Found' : '✗ Not Found'}\n"
    message += "Skin Model: ${skinOk ? '✓ Found' : '✗ Not Found'}\n"
    message += "Thorax Model: ${thoraxOk ? '✓ Found' : '✗ Not Found'}\n"
    
    if (pythonOk && (colorectalOk || skinOk || thoraxOk)) {
        Dialogs.showInfoNotification("Configuration Test", message)
    } else {
        Dialogs.showErrorMessage("Configuration Test", message)
    }
}

// Tutorial dialog
def showTutorial() {
    Platform.runLater {
        def dialog = new Dialog<ButtonType>()
        dialog.setTitle("SPIDER Tutorial")
        dialog.setHeaderText("How to Use SPIDER Models")

        def content = new VBox(10)
        content.setPadding(new Insets(10))
        def tutorialText = new TextArea(
"""SPIDER Workflow Tutorial:

STEP 1: Initial Setup
- Install Python and required packages
- Download SPIDER models from Hugging Face
- Configure paths in the Configuration menu

STEP 2: Prepare Your Slide
- Open your whole slide image (.svs) in QuPath
- Navigate to regions of interest
- Use annotation tools to mark areas

STEP 3: Classification Methods

Method A - Classify Annotations:
- Draw annotations around specific regions
- Select annotations to classify
- Run \"Classify Selected Annotations\"
- Best for: Quick classification of specific areas

Method B - Tile Analysis:
- Draw larger annotation covering heterogeneous tissue
- Run \"Tile Analysis\" 
- Creates 1120×1120 pixel grid
- Best for: Detailed spatial analysis

Method C - Whole Slide Analysis:
- No annotations needed
- Processes entire slide
- Generates heatmaps for each class
- Best for: Overview of tissue distribution

STEP 4: Interpret Results
- Classifications shown as colors
- Confidence scores in measurements
- Tile analysis shows tissue composition percentages

Tips:
- Start with small regions to test
- Use appropriate model for tissue type
- Check confidence scores for reliability
"""
        )
        tutorialText.setEditable(false)
        tutorialText.setWrapText(true)
        tutorialText.setPrefSize(500, 500)
        
        content.getChildren().add(tutorialText)
        
        dialog.getDialogPane().setContent(content)
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK)
        dialog.setResizable(true)
        dialog.showAndWait()
    }
}

// Run the classification script directly with parameters
def runClassifyAnnotations(String modelType) {
    println("Running classification with ${modelType} model")
    
    // Check configuration
    if (!SPIDERConfig.pythonPath || !SPIDERConfig.modelsBasePath) {
        Dialogs.showErrorMessage("Configuration Required", 
            "Please configure Python path and models directory first")
        createMainMenu()
        return
    }
    
    def modelInfo = getModelInfo()[modelType]
    def modelPath = Paths.get(SPIDERConfig.modelsBasePath, modelInfo.fullName).toString()
    
    // Check if model exists
    if (!new File(modelPath).exists()) {
        Dialogs.showErrorMessage("Model Not Found", 
            "Model not found at: ${modelPath}\n\nPlease download from Hugging Face")
        return
    }
    
    // Build all paths in the main script
    def projectPath = PROJECT_BASE_DIR
    def projectParent = new File(projectPath).getParent()
    if (projectParent == null) {
        projectParent = projectPath  // Use project path itself if no parent
    }
    
    def outputPath = buildFilePath(projectParent, "output", "classifications")
    def pythonScriptPath = buildFilePath(projectParent, "python", "spider_qupath_classifier.py")
    def tempAnnotationsPath = buildFilePath(outputPath, "annotations_to_predict.json")
    
    // Check if scripts exist
    if (!new File(pythonScriptPath).exists()) {
        Dialogs.showErrorMessage("Python Script Missing", "Required Python script not found: ${pythonScriptPath}")
        return
    }
    
    // Create output directory
    new File(outputPath).mkdirs()
    
    // Get selected annotations
    def annotations = getSelectedObjects().findAll { it.isAnnotation() }
    if (annotations.isEmpty()) {
        annotations = getAnnotationObjects().findAll { it.getPathClass() == null }
    }
    
    if (annotations.isEmpty()) {
        Dialogs.showErrorMessage("SPIDER Classification", "No annotations found to classify!")
        return
    }
    
    // Export annotations as JSON
    def imageData = getCurrentImageData()
    def server = imageData.getServer()
    def imagePath = server.getPath()
    def imageName = GeneralTools.stripExtension(server.getMetadata().getName())
    
    def annotationData = []
    annotations.each { annotation ->
        def roi = annotation.getROI()
        def x = roi.getBoundsX()
        def y = roi.getBoundsY()
        def width = roi.getBoundsWidth()
        def height = roi.getBoundsHeight()
        
        annotationData.add([
            id: annotation.getID(),
            slide_path: imagePath,
            image_name: imageName,
            roi: [
                x: x,
                y: y,
                width: width,
                height: height
            ]
        ])
    }
    
    // Save annotations as JSON
    def gson = GsonTools.getInstance(true)
    new File(tempAnnotationsPath).text = gson.toJson(annotationData)
    
    println("Exported ${annotationData.size()} annotations to ${tempAnnotationsPath}")
    
    // Run Python classifier
    def command = [SPIDERConfig.pythonPath, pythonScriptPath, tempAnnotationsPath, modelPath, outputPath]
    println("Running command: " + command.join(" "))
    
    def process = new ProcessBuilder(command)
        .redirectErrorStream(true)
        .start()
        
    // Read output
    def reader = new BufferedReader(new InputStreamReader(process.getInputStream()))
    def line
    while ((line = reader.readLine()) != null) {
        println(line)
    }
    
    def exitCode = process.waitFor()
    if (exitCode != 0) {
        Dialogs.showErrorMessage("Error", "SPIDER classification failed with exit code: ${exitCode}")
        return
    }
    
    // Load predictions
    def predictionsPath = buildFilePath(outputPath, "predictions.json")
    if (!new File(predictionsPath).exists()) {
        Dialogs.showErrorMessage("Error", "Predictions file not found: ${predictionsPath}")
        return
    }
    
    // Parse predictions
    def predictionsFile = new File(predictionsPath)
    def jsonParser = new JsonParser()
    def jsonArray = jsonParser.parse(predictionsFile.text).getAsJsonArray()
    
    // Create a map for quick lookup
    def annotationMap = [:]
    getAnnotationObjects().each { annotation ->
        annotationMap[annotation.getID().toString()] = annotation
    }
    
    // Apply classifications
    def applied = 0
    
    for (int i = 0; i < jsonArray.size(); i++) {
        def predObj = jsonArray.get(i).getAsJsonObject()
        def predictionId = predObj.get("id").getAsString()
        
        def annotation = annotationMap[predictionId]
        if (annotation && !predObj.get("prediction").isJsonNull()) {
            def className = predObj.get("prediction").getAsString()
            
            // Get or create class
            def pathClass = getPathClass(className)
            annotation.setPathClass(pathClass)
            
            // Get confidence
            def probsObj = predObj.get("probabilities").getAsJsonObject()
            def confidence = probsObj.get(className).getAsDouble()
            
            // Add confidence as annotation name
            def confidenceStr = String.format("%.1f", confidence * 100)
            annotation.setName("SPIDER: " + className + " (" + confidenceStr + "%)")
            
            // Add measurements for all probabilities
            def iterator = probsObj.entrySet().iterator()
            while (iterator.hasNext()) {
                def entry = iterator.next()
                def classKey = entry.getKey()
                def probability = entry.getValue().getAsDouble()
                annotation.measurements.put("SPIDER: P(" + classKey + ")", probability)
            }
            
            applied++
        }
    }
    
    // Update display
    if (applied > 0) {
        fireHierarchyUpdate()
        println("Applied classifications to " + applied + " annotations")
        Dialogs.showInfoNotification("SPIDER", "Successfully classified " + applied + " annotations")
    } else {
        Dialogs.showErrorMessage("SPIDER", "Failed to apply classifications")
    }
}

// Run tile analysis with parameters
def runTileAnalysis(String modelType) {
    println("Running tile analysis with ${modelType} model")
    
    // Check configuration
    if (!SPIDERConfig.pythonPath || !SPIDERConfig.modelsBasePath) {
        Dialogs.showErrorMessage("Configuration Required", 
            "Please configure Python path and models directory first")
        createMainMenu()
        return
    }
    
    def modelInfo = getModelInfo()[modelType]
    def modelPath = Paths.get(SPIDERConfig.modelsBasePath, modelInfo.fullName).toString()
    
    // Check if model exists
    if (!new File(modelPath).exists()) {
        Dialogs.showErrorMessage("Model Not Found", 
            "Model not found at: ${modelPath}\n\nPlease download from Hugging Face")
        return
    }
    
    // Define paths directly
    def projectPath = PROJECT_BASE_DIR
    def projectParent = new File(projectPath).getParent()
    if (projectParent == null) {
        projectParent = projectPath  // Use project path itself if no parent
    }
    
    def outputPath = buildFilePath(projectParent, "output", "classifications")
    def pythonScriptPath = buildFilePath(projectParent, "python", "spider_qupath_classifier.py")
    def tempAnnotationsPath = buildFilePath(outputPath, "tiles_to_predict.json")
    
    // Check if scripts exist
    if (!new File(pythonScriptPath).exists()) {
        Dialogs.showErrorMessage("Python Script Missing", "Required Python script not found: ${pythonScriptPath}")
        return
    }
    
    // Create output directory
    new File(outputPath).mkdirs()
    
    // Get selected annotations
    def selectedAnnotations = getSelectedObjects().findAll { it.isAnnotation() }
    
    if (selectedAnnotations.isEmpty()) {
        Dialogs.showErrorMessage("SPIDER Tiling", "Please select at least one annotation to process")
        return
    }
    
    println("Processing ${selectedAnnotations.size()} selected annotations")
    
    // Get current image data
    def imageData = getCurrentImageData()
    def server = imageData.getServer()
    def imagePath = server.getPath()
    def imageName = GeneralTools.stripExtension(server.getMetadata().getName())
    
    // GENERATE TILES
    println("\n--- STEP 1: GENERATING TILES ---")
    
    def patchSize = 1120
    def patchStride = 1120
    
    // List to hold all tiles
    def allTiles = []
    def tilesByAnnotation = [:]
    
    selectedAnnotations.eachWithIndex { annotation, annotationIndex ->
        def annotationROI = annotation.getROI()
        def annotationID = annotation.getID()
        
        // Get annotation bounds
        def startX = annotationROI.getBoundsX()
        def startY = annotationROI.getBoundsY()
        def width = annotationROI.getBoundsWidth()
        def height = annotationROI.getBoundsHeight()
        
        println("Annotation ${annotationIndex+1}/${selectedAnnotations.size()}: ${width}x${height} px")
        
        // Calculate number of tiles
        def numTilesX = Math.max(1, Math.floor(width / patchStride) as int)
        def numTilesY = Math.max(1, Math.floor(height / patchStride) as int)
        
        println("Creating ${numTilesX}x${numTilesY} = ${numTilesX * numTilesY} tiles")
        
        // Initialize list for this annotation
        def tilesForAnnotation = []
        tilesByAnnotation[annotationID] = tilesForAnnotation
        
        // Generate tiles
        for (int y = 0; y < numTilesY; y++) {
            for (int x = 0; x < numTilesX; x++) {
                // Calculate tile coordinates
                def tileX = startX + x * patchStride
                def tileY = startY + y * patchStride
                
                // Create rectangle ROI for this tile
                def tileROI = ROIs.createRectangleROI(
                    tileX, tileY, patchSize, patchSize, ImagePlane.getDefaultPlane())
                
                // Skip tiles that don't intersect with the annotation
                if (!annotationROI.getGeometry().intersects(tileROI.getGeometry()))
                    continue
                    
                // Create unique ID for this tile
                def tileID = "tile_${annotationID}_${x}_${y}"
                
                // Create tile object for JSON
                def tileObj = [
                    id: tileID,
                    slide_path: imagePath,
                    image_name: imageName,
                    parent_annotation_id: annotationID,
                    gridX: x,
                    gridY: y,
                    roi: [
                        x: tileX,
                        y: tileY,
                        width: patchSize,
                        height: patchSize
                    ]
                ]
                
                allTiles.add(tileObj)
                tilesForAnnotation.add(tileObj)
            }
        }
    }
    
    // Export tiles as JSON
    def gson = GsonTools.getInstance(true)
    new File(tempAnnotationsPath).text = gson.toJson(allTiles)
    
    println("Exported ${allTiles.size()} tiles to ${tempAnnotationsPath}")
    
    // RUN SPIDER CLASSIFICATION
    println("\n--- STEP 2: RUNNING SPIDER CLASSIFICATION ---")
    
    // Run SPIDER classifier Python script
    def command = [SPIDERConfig.pythonPath, pythonScriptPath, tempAnnotationsPath, modelPath, outputPath]
    println("Running command: " + command.join(" "))
    
    def process = new ProcessBuilder(command)
        .redirectErrorStream(true)
        .start()
    
    // Read and print the output
    def reader = new BufferedReader(new InputStreamReader(process.getInputStream()))
    def line
    while ((line = reader.readLine()) != null) {
        println(line)
    }
    
    def exitCode = process.waitFor()
    println("Python process finished with exit code: " + exitCode)
    
    if (exitCode != 0) {
        Dialogs.showErrorMessage("Error", "SPIDER classification failed. Check the log for details.")
        return
    }
    
    println("Classification completed successfully!")
    
    // PROCESS RESULTS
    println("\n--- STEP 3: PROCESSING RESULTS ---")
    
    // Load predictions
    def predictionsPath = buildFilePath(outputPath, "predictions.json")
    def predictionsFile = new File(predictionsPath)
    
    if (!predictionsFile.exists()) {
        Dialogs.showErrorMessage("Error", "Predictions file not found at " + predictionsPath)
        return
    }
    
    // Parse predictions
    def jsonParser = new JsonParser()
    def predictions = jsonParser.parse(predictionsFile.text).getAsJsonArray()
    
    // Create a map of tile predictions by ID for quick lookup
    def tileIdToPrediction = [:]
    for (int i = 0; i < predictions.size(); i++) {
        def predObj = predictions.get(i).getAsJsonObject()
        def tileId = predObj.get("id").getAsString()
        tileIdToPrediction[tileId] = predObj
    }
    
    println("Loaded ${predictions.size()} tile predictions")
    
    // VISUALIZE RESULTS
    println("\n--- STEP 4: VISUALIZING RESULTS ---")
    
    // Load class information from predictions
    def classSet = new HashSet<String>()
    for (int i = 0; i < predictions.size(); i++) {
        def predObj = predictions.get(i).getAsJsonObject()
        if (!predObj.get("prediction").isJsonNull()) {
            classSet.add(predObj.get("prediction").getAsString())
        }
    }
    def classes = classSet.toList()
    println("Found ${classes.size()} classes")
    
    // Get class abbreviations
    def classAbbrev = getClassAbbreviations(modelType)
    
    // Process results by annotation
    selectedAnnotations.each { annotation ->
        def annotationID = annotation.getID()
        
        // Get tiles for this annotation
        def annotationTiles = tilesByAnnotation[annotationID]
        if (annotationTiles == null || annotationTiles.isEmpty()) {
            println("No tiles found for annotation ${annotationID}")
            return
        }
        
        // Class distribution for statistics
        def classCount = [:]
        classes.each { classCount[it] = 0 }
        
        // Create tile annotations
        // Clear any existing tile annotations for this annotation
        def existingTiles = getAnnotationObjects().findAll { 
            it.getName() != null && it.getName().startsWith("Tile_${annotationID}_") 
        }
        removeObjects(existingTiles, true)
        
        // Create new tile annotations
        annotationTiles.each { tile ->
            def tileId = tile.id
            def predictionObj = tileIdToPrediction[tileId]
            
            if (predictionObj != null && !predictionObj.get("prediction").isJsonNull()) {
                // Get the classification
                def className = predictionObj.get("prediction").getAsString()
                def probsObj = predictionObj.get("probabilities").getAsJsonObject()
                def confidence = probsObj.get(className).getAsDouble()
                
                // Increment class count
                classCount[className] = (classCount[className] ?: 0) + 1
                
                // Create ROI for this tile
                def roi = ROIs.createRectangleROI(
                    tile.roi.x, tile.roi.y, tile.roi.width, tile.roi.height, 
                    ImagePlane.getDefaultPlane())
                
                // Create an annotation with the tile's class using PathObjects
                def pathClass = getPathClass(className)
                def tileAnnotation = PathObjects.createAnnotationObject(roi, pathClass)
                
                // Get abbreviated class name
                def abbrevName = classAbbrev[className] ?: className
                
                // Add a simple name with abbreviated class
                tileAnnotation.setName(abbrevName)
                
                // Add measurements as doubles to avoid casting errors
                tileAnnotation.measurements.put("SPIDER: Confidence", confidence * 100.0)
                
                // Store grid coordinates as doubles to avoid casting errors
                tileAnnotation.measurements.put("SPIDER: GridX", Double.valueOf(tile.gridX))
                tileAnnotation.measurements.put("SPIDER: GridY", Double.valueOf(tile.gridY))
                
                // Add to image data
                addObject(tileAnnotation)
            }
        }
        
        // Calculate class percentages
        def totalPredicted = classCount.values().sum()
        def classPercentages = [:]
        if (totalPredicted > 0) {
            classCount.each { cls, count ->
                classPercentages[cls] = count / totalPredicted
            }
        }
        
        // Add summary to annotation
        def summaryStr = new StringBuilder("SPIDER tile summary:\n")
        
        // Find top 3 classes
        if (totalPredicted > 0) {
            def sortedClasses = classCount.entrySet()
                .sort { -it.value }
                .take(3)
                .collect { "${it.key}: ${it.value} (${(classPercentages[it.key] * 100).round(1)}%)" }
                .join(", ")
            
            summaryStr.append(sortedClasses)
        } else {
            summaryStr.append("No classes detected")
        }
        
        // Add all class percentages as measurements to the annotation
        classPercentages.each { cls, percentage ->
            // Store as double to avoid casting issues
            annotation.measurements.put("SPIDER: " + cls + " %", Double.valueOf(percentage * 100.0))
        }
        
        // Set annotation description
        annotation.setDescription(summaryStr.toString())
        
        println("Processed ${annotationTiles.size()} tiles for annotation ${annotationID}")
        println("Class distribution: ${classCount}")
    }
    
    // Clean up temporary files
    // new File(tempAnnotationsPath).delete()
    
    // Update display
    fireHierarchyUpdate()
    
    println("Completed SPIDER tile classification")
    
    // Create a summary dialog
    Dialogs.showInfoNotification(
        "SPIDER Tile Classification",
        "Successfully classified ${allTiles.size()} tiles from ${selectedAnnotations.size()} annotations"
    )
}

// Show whole slide analysis dialog
def showWholeSlideDialog(String modelType) {
    Platform.runLater {
        def dialog = new Dialog<ButtonType>()
        dialog.setTitle("Whole Slide Analysis")
        dialog.setHeaderText("Configure Whole Slide Analysis")
        
        def grid = new GridPane()
        grid.setHgap(10)
        grid.setVgap(10)
        grid.setPadding(new Insets(20))
        
        // Output directory
        def outputField = new TextField()
        outputField.setPrefWidth(300)
        
        // Default output directory is in the project/output/whole_slide folder
        def defaultOutputPath = buildFilePath(PROJECT_BASE_DIR, "output", "whole_slide")
        new File(defaultOutputPath).mkdirs() // Create the directory
        outputField.setText(defaultOutputPath)
        
        def outputBtn = new Button("Browse...")
        outputBtn.setOnAction {
            def chooser = new DirectoryChooser()
            chooser.setTitle("Select Output Directory")
            chooser.setInitialDirectory(new File(defaultOutputPath))
            def dir = chooser.showDialog(dialog.getOwner())
            if (dir != null) {
                outputField.setText(dir.getAbsolutePath())
            }
        }
        
        // Parameters
        def strideField = new TextField("560")  // Default = 50% overlap
        def maxPatchesField = new TextField("1000")  // Memory limit
        def workersField = new TextField("4")  // Number of CPU cores
        
        grid.add(new Label("Output Directory:"), 0, 0)
        grid.add(outputField, 1, 0)
        grid.add(outputBtn, 2, 0)
        
        grid.add(new Label("Patch Stride:"), 0, 1)
        grid.add(strideField, 1, 1)
        grid.add(new Label("(560 = 50% overlap)"), 2, 1)
        
        grid.add(new Label("Max Patches:"), 0, 2)
        grid.add(maxPatchesField, 1, 2)
        grid.add(new Label("(limit for memory)"), 2, 2)
        
        grid.add(new Label("Workers:"), 0, 3)
        grid.add(workersField, 1, 3)
        grid.add(new Label("(parallel processing)"), 2, 3)
        
        dialog.getDialogPane().setContent(grid)
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)
        
        def result = dialog.showAndWait()
        if (result.isPresent() && result.get() == ButtonType.OK) {
            runWholeSlideAnalysis(modelType, outputField.getText(), 
                strideField.getText(), maxPatchesField.getText(), workersField.getText())
        }
    }
}

// Run whole slide analysis
def runWholeSlideAnalysis(String modelType, String outputDir, String stride, String maxPatches, String workers) {
    // Check configuration
    if (!SPIDERConfig.pythonPath || !SPIDERConfig.modelsBasePath) {
        Dialogs.showErrorMessage("Configuration Required", 
            "Please configure Python path and models directory first")
        return
    }
    
    // Get model path
    def modelInfo = getModelInfo()[modelType]
    def modelPath = Paths.get(SPIDERConfig.modelsBasePath, modelInfo.fullName).toString()
    if (!new File(modelPath).exists()) {
        Dialogs.showErrorMessage("Model Not Found", 
            "Model not found at: ${modelPath}\n\nPlease download from Hugging Face")
        return
    }
    
    // Get slide path
    def slidePath = getCurrentImageData().getServer().getPath()
    
    // Convert from QuPath path format if needed
    if (slidePath.startsWith("file:")) {
        slidePath = slidePath.substring(slidePath.indexOf("file:") + 5)
        while (slidePath.startsWith("/")) {
            slidePath = slidePath.substring(1)
        }
    }
    
    // Find the Python script
    def projectPath = PROJECT_BASE_DIR
    def projectParent = new File(projectPath).getParent()
    if (projectParent == null) {
        projectParent = projectPath  // Use project path itself if no parent
    }
    
    def pythonScriptPath = buildFilePath(projectParent, "python", "whole_slide_analysis_spider_universal.py")
    
    // If the universal script doesn't exist, try model-specific script
    if (!new File(pythonScriptPath).exists()) {
        pythonScriptPath = buildFilePath(projectParent, "python", "whole_slide_analysis_spider_${modelType}.py")
        
        if (!new File(pythonScriptPath).exists()) {
            Dialogs.showErrorMessage("Python Script Missing", 
                "Required Python script not found: ${pythonScriptPath}")
            return
        }
    }
    
    // Create output directory if it doesn't exist
    new File(outputDir).mkdirs()
    
    // Build command
    def command = [
        SPIDERConfig.pythonPath,
        pythonScriptPath,
        modelPath,
        slidePath,
        outputDir,
        stride,
        maxPatches,
        workers
    ]
    
    println("Running: " + command.join(" "))
    
    // Run in background
    def process = new ProcessBuilder(command)
        .redirectErrorStream(true)
        .start()
    
    // Start a thread to read and display output
    Thread.start {
        def reader = new BufferedReader(new InputStreamReader(process.getInputStream()))
        def line
        while ((line = reader.readLine()) != null) {
            println(line)
        }
        
        def exitCode = process.waitFor()
        if (exitCode == 0) {
            Platform.runLater {
                Dialogs.showInfoNotification("Whole Slide Analysis", 
                    "Analysis completed! Results saved to: ${outputDir}")
            }
        } else {
            Platform.runLater {
                Dialogs.showErrorMessage("Whole Slide Analysis Error", 
                    "Analysis failed with exit code ${exitCode}. Check log for details.")
            }
        }
    }
    
    Dialogs.showInfoNotification("Whole Slide Analysis Started", 
        "Processing slide... Results will be saved to:\n${outputDir}")
}

// Install plugin menu in QuPath
try {
    def gui = getQuPath()
    def menu = gui.getMenu("Extensions>SPIDER Analysis", true)
    menu.getItems().clear()
    def menuItem = new MenuItem("Open SPIDER Menu")
    menuItem.setOnAction { createMainMenu() }
    menu.getItems().add(menuItem)
    menuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+S"))
    println("SPIDER Plugin installed! Access via Extensions > SPIDER Analysis or press Ctrl+Shift+S")
} catch (Exception e) {
    println("Could not install menu item: " + e.getMessage())
}

// Launch menu on load
createMainMenu()