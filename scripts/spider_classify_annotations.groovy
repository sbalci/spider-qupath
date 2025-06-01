// spider_classify_annotations.groovy
// Working Complete Workflow: Export annotations, run SPIDER model, and apply classifications

import qupath.lib.objects.PathObject
import qupath.lib.roi.RectangleROI
import qupath.lib.io.GsonTools
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.objects.classes.PathClassFactory
import com.google.gson.JsonParser

// Start timing
def startTime = System.currentTimeMillis()
println("INFO: Starting SPIDER classification workflow at " + new Date())

// Define paths
def projectPath = buildFilePath(PROJECT_BASE_DIR)
def pythonPath = "D:\\DigitalPathologyDrafts\\.venv\\Scripts\\python.exe"  // Update this to your Python path
def outputPath = new File(new File(projectPath).getParent(), "output/classifications").getAbsolutePath()
def scriptPath = new File(new File(projectPath).getParent(), "python/spider_qupath_classifier.py").getAbsolutePath()
def modelPath = "D:\\histai\\SPIDER-colorectal-model"  // Update this to your SPIDER model path
def tempAnnotationsPath = buildFilePath(outputPath, "annotations_to_predict.json")

// Create output directory
def outputDir = new File(outputPath)
if (!outputDir.exists())
    outputDir.mkdirs()

// STEP 1: EXPORT ANNOTATIONS
println("\n--- STEP 1: EXPORTING ANNOTATIONS ---")

// Get current image data
def imageData = getCurrentImageData()
def server = imageData.getServer()
def imagePath = server.getPath()
def imageName = GeneralTools.stripExtension(server.getMetadata().getName())

// Get unclassified annotations
def annotations = getAnnotationObjects().findAll { it.getPathClass() == null }
println("Found " + annotations.size() + " unclassified annotations")

if (annotations.isEmpty()) {
    Dialogs.showErrorMessage("SPIDER Classification", "No unclassified annotations found!")
    return
}

// Prepare annotation data for export
def annotationData = []

annotations.each { annotation ->
    // Get annotation properties
    def roi = annotation.getROI()
    def x = roi.getBoundsX()
    def y = roi.getBoundsY()
    def width = roi.getBoundsWidth()
    def height = roi.getBoundsHeight()
    
    // Create annotation object for JSON
    def annotationObj = [
        id: annotation.getID(),
        slide_path: imagePath,
        image_name: imageName,
        roi: [
            x: x,
            y: y,
            width: width,
            height: height
        ]
    ]
    
    annotationData.add(annotationObj)
}

// Export annotations as JSON using GsonTools
def gson = GsonTools.getInstance(true)
def jsonString = gson.toJson(annotationData)
new File(tempAnnotationsPath).text = jsonString

println("Exported " + annotationData.size() + " annotations to " + tempAnnotationsPath)

// STEP 2: RUN SPIDER CLASSIFICATION
println("\n--- STEP 2: RUNNING SPIDER CLASSIFICATION ---")

// Run SPIDER classifier
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

// STEP 3: APPLY CLASSIFICATIONS
println("\n--- STEP 3: APPLYING CLASSIFICATIONS ---")

// Load predictions
def predictionsPath = buildFilePath(outputPath, "predictions.json")
def predictionsFile = new File(predictionsPath)

if (!predictionsFile.exists()) {
    Dialogs.showErrorMessage("Error", "Predictions file not found at " + predictionsPath)
    return
}

def jsonText = predictionsFile.text
def jsonParser = new JsonParser()
def jsonArray = jsonParser.parse(jsonText).getAsJsonArray()

// Convert to a list structure we can work with
def predictions = []
for (int i = 0; i < jsonArray.size(); i++) {
    def predictionObj = jsonArray.get(i).getAsJsonObject()
    def prediction = [
        id: predictionObj.get("id").getAsString()
    ]
    
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
}

println("Loaded " + predictions.size() + " predictions")

// Get all annotations (not just unclassified ones)
def allAnnotations = getAnnotationObjects()

// Function to find or create a PathClass by name
def findOrCreatePathClass = { String name ->
    def pathClass = PathClassFactory.getPathClass(name)
    if (pathClass == null) {
        // Create default colors based on class types
        def color = null
        if (name.toLowerCase().contains("adenocarcinoma")) {
            color = getColorRGB(220, 20, 60) // Crimson
        } else if (name.toLowerCase().contains("adenoma")) {
            color = getColorRGB(255, 140, 0) // Dark Orange
        } else if (name.toLowerCase().contains("stroma")) {
            color = getColorRGB(50, 205, 50) // Lime Green
        } else if (name.toLowerCase().contains("muscle")) {
            color = getColorRGB(139, 0, 139) // Dark Magenta
        } else if (name.toLowerCase().contains("vessel")) {
            color = getColorRGB(0, 0, 255)  // Blue
        } else if (name.toLowerCase().contains("inflammation")) {
            color = getColorRGB(255, 255, 0) // Yellow
        } else if (name.toLowerCase().contains("fat")) {
            color = getColorRGB(255, 215, 0) // Gold
        } else if (name.toLowerCase().contains("necrosis")) {
            color = getColorRGB(128, 0, 0) // Maroon
        } else {
            color = getColorRGB(128, 128, 128) // Gray default
        }
        pathClass = PathClassFactory.getPathClass(name, color)
    }
    return pathClass
}

// Map by ID for quick lookup
def annotationMap = [:]
allAnnotations.each { annotation ->
    annotationMap[annotation.getID().toString()] = annotation
}

// Apply classes based on predictions
def applied = 0

predictions.each { prediction ->
    def annotation = annotationMap[prediction.id]
    if (annotation != null && prediction.prediction != null) {
        // Get or create the class
        def pathClass = findOrCreatePathClass(prediction.prediction)
        
        // Apply class
        annotation.setPathClass(pathClass)
        
        // Add confidence information as a comment
        def confidence = String.format("%.1f", prediction.probabilities[prediction.prediction] * 100)
        def comment = "SPIDER: " + prediction.prediction + " (" + confidence + "%)"
        annotation.setName(comment)
        
        // Add measurements for probabilities
        prediction.probabilities.each { className, probability ->
            annotation.measurements.put("SPIDER: P(" + className + ")", probability)
        }
        
        applied++
        
        // Debug info
        println("Applied class " + prediction.prediction + " to annotation " + prediction.id)
    } else {
        if (annotation == null) {
            println("WARNING: Could not find annotation with ID " + prediction.id)
        }
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

// Show total time
def totalTimeSeconds = (System.currentTimeMillis() - startTime) / 1000
println("INFO: Total run time: " + String.format("%.2f", totalTimeSeconds) + " seconds")