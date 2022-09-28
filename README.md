# Artiference Android Demo

### Overview

This is a camera app that continuously detects the objects (bounding boxes, classes, lines, masks etc.) in the frames seen by your device's back camera, using **yolov3, mask-rcnn, posenet** models. 

It is recommended that the application be run on the device rather than on the emulator.

## Build the demo using Android Studio

### Prerequisites

- If you don't have already, install **[Android Studio](https://developer.android.com/studio/index.html)**, following the instructions on the website.
- You need an Android device and Android development environment with minimum API 26.
- Android Studio 3.2 or later.

### Building

- Open Android Studio, and from the Welcome screen, select Open an existing Android Studio project. Navigate down to 'reference-app' and click OK. 
- If it asks you to do a Gradle Sync, click OK.
- Click the Run button (the green arrow) or select Run > Run 'android' from the top menu. You may need to rebuild the project using Build > Rebuild Project.
- The install may fail at first, please press Run again to try.
- Or, it may be temporary, so please try cycling the application without having to reinstall it.

### Model used (important) 

<u>You have to explicitly download the model</u> from <u>google-drive</u> or, 

- yolov3: **[yolov3.weights](https://pjreddie.com/media/files/yolov3.weights)**

- posenet:  **[pose_iter_440000.caffemodel](http://posefs1.perception.cs.cmu.edu/Users/ZheCao/pose_iter_440000.caffemodel)**

- segmentation:  **[frozen_inference_graph.pb](http://download.tensorflow.org/models/object_detection/mask_rcnn_inception_v2_coco_2018_01_28.tar.gz)**

Extract the zip(if compressed) to get the **frozen_inference_graph.pb**, **pose_iter_440000.caffemodel**, **yolov3.weights** and put them in 'reference-app\app\assets' folder. 

### Additional Note

_Please do not delete the assets folder content_. In particular, do not erase a file that says __'mask_rcnn_inception.pbtxt'__, __'pose_deploy.prototxt'__,__'yolov3.cfg'__.  These are the configuration files that describe the structure of each model.

If you explicitly deleted the files, then please re-download from <u>google-drive</u> or copy from, 

- yolov3: **[yolov3.cfg](https://github.com/pjreddie/darknet/blob/master/cfg/yolov3.cfg)**

- posenet: **[pose_deploy_linevec.prototxt](https://github.com/spmallick/learnopencv/blob/master/OpenPose-Multi-Person/pose/coco/pose_deploy_linevec.prototxt)** 

  **>  rename it to 'pose_deploy.prototxt'**

- segmentation: **[mask_rcnn_inception_v2_coco_2018_01_28.pbtxt](https://github.com/spmallick/learnopencv/blob/master/Mask-RCNN/mask_rcnn_inception_v2_coco_2018_01_28.pbtxt)**

  **> rename it to 'mask_rcnn_inception.pbtxt'**