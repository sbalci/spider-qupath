// export_annotations.groovy
// Export unclassified annotations for SPIDER classification

import qupath.lib.objects.PathObject
import qupath.lib.roi.RectangleROI
import qupath.lib.io.GsonTools

// Define paths
def projectPath = buildFilePath(PROJECT_BASE_DIR)
def outputPath = buildFilePath(projectPath, "spider_output")
def tempAnnotationsPath = buildFilePath(outputPath, "annotations_to_predict.json")

// Create output directory
def outputDir = new File(outputPath)
if (!outputDir.exists())
    outputDir.mkdirs()

// Get current image data
def imageData = getCurrentImageData()
def server = imageData.getServer()
def imagePath = server.getPath()
def imageName = GeneralTools.stripExtension(server.getMetadata().getName())

// Get unclassified annotations
def annotations = getAnnotationObjects().findAll { it.getPathClass() == null }
println("Found " + annotations.size() + " unclassified annotations")

if (annotations.isEmpty()) {
    Dialogs.showErrorMessage("Export Annotations", "No unclassified annotations found!")
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
println("Next step: Run SPIDER classification script on the exported annotations")