# Driving Assistant for Android
The overall objective of this project is to design and develop an Android application that aids drivers in traffic by providing some signals to help them to drive in a safe manner. This app also helps to bridge the gap between modern vehicles with AI features and older vehicles where these features are absent. I had to design the app in a way so that the delay between receiving input and producing a result is minimal. This was a key point to consider since it is meant to be used in a real traffic environment and any delay could cause an error leading to an accident.

Another aim was to minimise the amount of user interaction with the app. Audio feedback can be implemented so that it will not require the user to look at their device. The driver should be focused solely on driving, even when the app is being used. The setup and user interface had to be very simple in order to achieve this.

Using OpenCV, the app processes the video captured from the device's camera in order identify the current and lane drift. Optical Character Recognition was used to determine the current speed limit from the detected speed signs.

[Video demos](https://www.youtube.com/playlist?list=PLhElOp3DFpOejprstGSl3eXzA8RrNvVGp)

[Google Play Store](https://play.google.com/store/apps/details?id=ycc.androiddrivingassistant)
