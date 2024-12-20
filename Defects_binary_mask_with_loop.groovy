// Hyper parameters
String label_name = "Other" // Annotation selection name
double downsample = 1 // Define how much to downsample during export (may be required for large images)
int tiling_factor = 10 // Size of image to size of the tile
String format = "png" // Mask format
String name = "Defects" // Name of the mask


// Detect the current image data
def imageData = getCurrentImageData()

// Define output path (relative to project)
def outputDir = buildFilePath(PROJECT_BASE_DIR, 'export')

// Create the output directory
mkdirs(outputDir)

// Extract the name of the image
def img_name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())

// Get the current image server
def img = getCurrentServer()

// Get the image width and height in pixels
int img_width = img.getWidth()
int img_height = img.getHeight()

// Define the tile dimensions as image size divided by the tiling factor
int tile_width = img_width.intdiv(tiling_factor)
int tile_height = img_height

// Print the results
print "Image size: " + "(" + img_width + ", " + img_height + ")"  + " px"
print "Tile size: " + "(" + tile_width + ", " + tile_height + ")"  + " px"

// Get the image plane
def plane = ImagePlane.getDefaultPlane()

// Correct the maximum height and width to avoid missing the image boundaries
corrector = (tiling_factor - 1) / (tiling_factor)
maximum_height = img_height - corrector*tile_height
maximum_width = img_width - corrector*tile_width

print "Classifying the slide"
counter = 1
for (int y = 0; y < maximum_height ; y += tile_height) {
    for (int x = 0; x < maximum_width; x += tile_width) {
        // Define the tile number
        String tile_number = "(" + counter + "/" + tiling_factor + "): "
        
        // file name
        def file_name = img_name + "_" + name + "_mask_" + counter + "_of_" + tiling_factor + "." + format
        
        // Check if the file already exist
        if (new File(outputDir, file_name).exists()) {
            print tile_number + "Already exist"
        } else {
            // Create a rectangle ROI
            def roi = ROIs.createRectangleROI(x, y, tile_width, tile_height, plane)
            
            // Create an annotation object from the ROI
            def annotation = PathObjects.createAnnotationObject(roi)
            
            // Set the ROI as the current selection
            setSelectedObject(annotation)
            
            // Add the annotation object to the annotations
            addObject(annotation)
            
            // Create an ImageServer where the pixels are derived from annotations
            print tile_number + "Creating mask server"
            def labelServer = new LabeledImageServer.Builder(imageData)
              .backgroundLabel(0, ColorTools.WHITE) // Specify background label (usually 0 or 255)
              .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported
              .addLabel(label_name, 255)      // Choose output labels (the order matters!)
              .multichannelOutput(false) // If true, each label refers to the channel of a multichannel binary image (required for multiclass probability)
              .build()
            
            // Create the output path
            def path = buildFilePath(outputDir, file_name)
            
            // Export the labeled image of the selected object
            def region = RegionRequest.createInstance(labelServer.getPath(), 1, selectedObject.getROI())
            
            // Write the mask
            print tile_number + "Writing mask"
            writeImageRegion(labelServer, region, path)
            
            // Delete the annotation
            print tile_number + "Removing annotations"
            removeObject(annotation, false)
        }
        
        // Increase the counter
        counter++
    }
}

// Announce a termination of the script
print "Done!"