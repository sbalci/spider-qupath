// SPIDER QuPath Classifier
// This script divides selected annotations into patches, applies SPIDER model classification,
// and creates a clean visualization with abbreviated class names

import qupath.lib.objects.PathObject
import qupath.lib.roi.RectangleROI
import qupath.lib.geom.Point2
import qupath.lib.regions.ImagePlane
import qupath.lib.roi.ROIs
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.io.GsonTools
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.objects.classes.PathClassFactory
import javafx.scene.paint.Color

// Configuration
def pythonPath = "D:\\DigitalPathologyDrafts\\.venv\\Scripts\\python.exe"  // Update to your Python path if needed
def modelPath = "D:\\histai\\SPIDER-colorectal-model"  // Update to your model path
def patchSize = 1120  // SPIDER model input size
def patchStride = 1120  // No overlap with stride=patchSize
def visualizeResults = true
def keepTempFiles = false

// Abbreviated class name map (keep these short for cleaner display)
def classAbbreviations = [
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

// Start timing
def startTime = System.currentTimeMillis()
println("Starting SPIDER tile classification workflow at " + new Date())

// Define paths
def projectPath = buildFilePath(PROJECT_BASE_DIR)
def outputPath = buildFilePath(projectPath, "spider_output")
def scriptPath = buildFilePath(projectPath, "spider_qupath_classifier.py")
def tempAnnotationsPath = buildFilePath(outputPath, "tiles_to_predict.json")

// Create output directory
def outputDir = new File(outputPath)
if (!outputDir.exists())
    outputDir.mkdirs()

// Get current image data
def imageData = getCurrentImageData()
def server = imageData.getServer()
def imagePath = server.getPath()
def imageName = GeneralTools.stripExtension(server.getMetadata().getName())
def pixelSizeMicrons = server.getPixelCalibration().getAveragedPixelSizeMicrons()

// Get selected annotations (or prompt to select one)
def selectedAnnotations = getSelectedObjects().findAll { it.isAnnotation() }

if (selectedAnnotations.isEmpty()) {
    Dialogs.showErrorMessage("SPIDER Tiling", "Please select at least one annotation to process")
    return
}

println("Processing ${selectedAnnotations.size()} selected annotations")

// STEP 1: GENERATE TILES
println("\n--- STEP 1: GENERATING TILES ---")

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
def jsonString = gson.toJson(allTiles)
new File(tempAnnotationsPath).text = jsonString

println("Exported ${allTiles.size()} tiles to ${tempAnnotationsPath}")

// STEP 2: RUN SPIDER CLASSIFICATION
println("\n--- STEP 2: RUNNING SPIDER CLASSIFICATION ---")

// Run SPIDER classifier Python script
def command = [pythonPath, scriptPath, tempAnnotationsPath, modelPath, outputPath]
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

// STEP 3: PROCESS RESULTS
println("\n--- STEP 3: PROCESSING RESULTS ---")

// Load predictions
def predictionsPath = buildFilePath(outputPath, "predictions.json")
def predictionsFile = new File(predictionsPath)

if (!predictionsFile.exists()) {
    Dialogs.showErrorMessage("Error", "Predictions file not found at " + predictionsPath)
    return
}

def jsonText = predictionsFile.text
def jsonParser = new com.google.gson.JsonParser()
def jsonArray = jsonParser.parse(jsonText).getAsJsonArray()

// Convert to a list structure
def predictions = []
for (int i = 0; i < jsonArray.size(); i++) {
    try {
        def predictionObj = jsonArray.get(i).getAsJsonObject()
        
        // Handle ID format - using Groovy's dynamic typing 
        def id
        def idElement = predictionObj.get("id")
        if (idElement.isJsonPrimitive()) {
            id = idElement.getAsString()
        } else if (idElement.isJsonObject()) {
            // Extract the ID components without needing JsonObject class
            def idObj = idElement.getAsJsonObject()
            def values = []
            if (idObj.has("values") && idObj.get("values").isJsonArray()) {
                def valuesArray = idObj.get("values").getAsJsonArray()
                for (int j = 0; j < valuesArray.size(); j++) {
                    values.add(valuesArray.get(j).toString().replaceAll('"', ''))
                }
            }
            id = "tile_${values[0]}_${values[1]}_${values[2]}"
        } else {
            id = "prediction_${i}"
        }
        
        def prediction = [id: id]
        
        if (!predictionObj.get("prediction").isJsonNull()) {
            prediction.prediction = predictionObj.get("prediction").getAsString()
            
            // Extract probabilities
            def probsObj = predictionObj.get("probabilities").getAsJsonObject()
            def probs = [:]
            probsObj.keySet().each { className ->
                probs[className] = probsObj.get(className).getAsDouble()
            }
            prediction.probabilities = probs
        } else {
            prediction.prediction = null
            prediction.probabilities = null
        }
        
        predictions.add(prediction)
    } catch (Exception e) {
        println("Error parsing prediction ${i}: ${e.getMessage()}")
    }
}

println("Loaded ${predictions.size()} tile predictions")

// STEP 4: VISUALIZE RESULTS
println("\n--- STEP 4: VISUALIZING RESULTS ---")

// Load class information from predictions
def classSet = new HashSet<String>()
predictions.each { prediction ->
    if (prediction.prediction != null) {
        classSet.add(prediction.prediction)
    }
}
def classes = classSet.toList()
println("Found ${classes.size()} classes")

// Helper function to create distinct colors for classes
def getClassColors = { classNames ->
    def colorMap = [:]
    
    // Define distinctive colors using RGB for better separation
    def baseColors = [
        // Adenocarcinoma - reds
        "Adenocarcinoma high grade": Color.rgb(220, 20, 60, 0.6),  // Crimson with alpha
        "Adenocarcinoma low grade": Color.rgb(255, 69, 0, 0.6),    // Red-orange with alpha
        
        // Adenoma - oranges
        "Adenoma high grade": Color.rgb(255, 140, 0, 0.6),         // Dark orange with alpha
        "Adenoma low grade": Color.rgb(255, 165, 0, 0.6),          // Orange with alpha
        
        // Stroma/Structure - greens
        "Stroma healthy": Color.rgb(50, 205, 50, 0.6),             // Lime green with alpha
        "Hyperplastic polyp": Color.rgb(0, 128, 0, 0.6),           // Green with alpha
        "Sessile serrated lesion": Color.rgb(0, 250, 154, 0.6),    // Medium spring green with alpha
        
        // Fat/Muscle - purples/yellows
        "Fat": Color.rgb(255, 215, 0, 0.6),                        // Gold with alpha
        "Muscle": Color.rgb(139, 0, 139, 0.6),                     // Dark magenta with alpha
        
        // Vessels/Fluids - blues
        "Vessels": Color.rgb(0, 0, 255, 0.6),                      // Blue with alpha
        "Mucus": Color.rgb(0, 191, 255, 0.6),                      // Deep sky blue with alpha
        
        // Inflammation/Necrosis - yellows/browns
        "Inflammation": Color.rgb(255, 255, 0, 0.6),               // Yellow with alpha
        "Necrosis": Color.rgb(128, 0, 0, 0.6)                      // Maroon with alpha
    ]
    
    classNames.each { className ->
        // Use predefined color if available, otherwise generate one
        if (baseColors.containsKey(className)) {
            colorMap[className] = baseColors[className]
        } else {
            // Generate a color based on the hash of the class name
            def hash = className.hashCode()
            def r = (hash & 0xFF0000) >> 16
            def g = (hash & 0x00FF00) >> 8
            def b = hash & 0x0000FF
            colorMap[className] = Color.rgb(r, g, b, 0.6)  // Add alpha for transparency
        }
    }
    
    return colorMap
}

// Create color map for classes
def classColorMap = getClassColors(classes)

// Function to find or create a PathClass by name with our color map
def findOrCreatePathClass = { String name ->
    def pathClass = PathClassFactory.getPathClass(name)
    if (pathClass == null) {
        // Use our color map
        def color = classColorMap[name]
        if (color == null) {
            color = Color.rgb(128, 128, 128, 0.6) // Gray default with alpha
        }
        pathClass = PathClassFactory.getPathClass(name, color)
    }
    return pathClass
}

// Create class map for statistics
def allPathClasses = classes.collect { findOrCreatePathClass(it) }

// Process results by annotation
selectedAnnotations.each { annotation ->
    def annotationID = annotation.getID()
    
    // Get tiles for this annotation
    def annotationTiles = tilesByAnnotation[annotationID]
    if (annotationTiles == null || annotationTiles.isEmpty()) {
        println("No tiles found for annotation ${annotationID}")
        return
    }
    
    // Get predictions for these tiles
    def tileIdToPrediction = [:]
    
    predictions.each { prediction ->
        def tileId = prediction.id
        if (tileId.startsWith("tile_${annotationID}_")) {
            tileIdToPrediction[tileId] = prediction
        }
    }
    
    // Class distribution for statistics
    def classCount = [:]
    classes.each { classCount[it] = 0 }
    
    // Create tile annotations if visualization is enabled
    if (visualizeResults) {
        // Clear any existing tile annotations for this annotation
        def existingTiles = getAnnotationObjects().findAll { 
            it.getName() != null && it.getName().startsWith("Tile_${annotationID}_") 
        }
        removeObjects(existingTiles, true)
        
        // Create new tile annotations
        annotationTiles.each { tile ->
            def tileId = tile.id
            def prediction = tileIdToPrediction[tileId]
            
            if (prediction != null && prediction.prediction != null) {
                // Get the classification
                def className = prediction.prediction
                def confidence = prediction.probabilities[className]
                
                // Increment class count
                classCount[className] = (classCount[className] ?: 0) + 1
                
                // Create ROI for this tile
                def roi = ROIs.createRectangleROI(
                    tile.roi.x, tile.roi.y, tile.roi.width, tile.roi.height, 
                    ImagePlane.getDefaultPlane())
                
                // Create an annotation with the tile's class
                def pathClass = findOrCreatePathClass(className)
                def tileAnnotation = new PathAnnotationObject(roi, pathClass)
                
                // Get abbreviated class name
                def abbrevName = classAbbreviations[className] ?: className
                
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
    } else {
        // Just count the classes
        annotationTiles.each { tile ->
            def tileId = tile.id
            def prediction = tileIdToPrediction[tileId]
            
            if (prediction != null && prediction.prediction != null) {
                // Increment class count
                def className = prediction.prediction
                classCount[className] = (classCount[className] ?: 0) + 1
            }
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
if (!keepTempFiles) {
    if (new File(tempAnnotationsPath).delete())
        println("Deleted temporary annotation file")
}

// Update display
fireHierarchyUpdate()

// Set annotation display properties for better visualization
setAnnotationDisplayProperties()

println("Completed SPIDER tile classification")

// Show total time
def totalTimeSeconds = (System.currentTimeMillis() - startTime) / 1000
println("Total run time: " + String.format("%.2f", totalTimeSeconds) + " seconds")

// Create a summary dialog
Dialogs.showInfoNotification(
    "SPIDER Tile Classification",
    "Successfully classified ${allTiles.size()} tiles from ${selectedAnnotations.size()} annotations"
)

// Function to set annotation display properties for cleaner visualization
void setAnnotationDisplayProperties() {
    try {
        // Try to access overlay options via the viewer
        def viewer = getCurrentViewer()
        if (viewer != null) {
            // Set text transparency and display properties
            def overlayOptions = viewer.getOverlayOptions()
            
            // Set text transparency to 60% 
            overlayOptions.setFillAlpha(0.6)
            
            // Set thin border around annotations
            overlayOptions.setLineThickness(2)
            
            // Center text and set smaller font for cleaner display
            overlayOptions.setFontSize(10.0)
            overlayOptions.setCenterAnnotationText(true)
            
            // Force update of display
            viewer.repaint()
        }
        
        // Try to set global preferences if possible
        try {
            def prefsClass = Class.forName("qupath.lib.gui.prefs.PathPrefs")
            
            // Set text opacity
            def opacityMethod = prefsClass.getMethod("textOpacityProperty")
            def opacityProp = opacityMethod.invoke(null)
            opacityProp.set(0.6)
            
            // Set font size
            def fontSizeMethod = prefsClass.getMethod("annotationFontSizeProperty")
            def fontSizeProp = fontSizeMethod.invoke(null)
            fontSizeProp.set(10.0)
            
            println("Set display properties for cleaner visualization")
        } catch (Exception ignored) {
            // Silently continue if preferences can't be set
        }
    } catch (Exception e) {
        println("Could not set annotation display properties: " + e.getMessage())
    }
}