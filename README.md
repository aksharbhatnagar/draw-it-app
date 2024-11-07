# Drawing/Animation Android App

A simple yet powerful Android app for creating drawings and animations. This app allows users to draw on a canvas, create multiple frames, and animate their artwork. It features essential tools for drawing, frame management, and animation playback.

## Features

### 1. **Canvas Area**
   - A **working area** for drawing, resembling a piece of paper with a background (taken from layouts).
   - The canvas supports **multi-frame drawing** where each frame can have its own drawing.
   - **Semi-transparent sketches** of the previous frame will be shown on subsequent frames to help guide the user.

### 2. **Drawing Tools**
   - **Pencil Tool**: Draws lines that follow the user's finger with the chosen colour.
   - **Eraser Tool**: Erases the traces the pencil leaves, following the user's finger.
   - Users can easily switch between the pencil and eraser tools, allowing for quick editing.

### 3. **Color Control**
   - Users can choose from at least **3 different colours** for the pencil tool.
   - Easily switch between colours to create diverse drawings and animations.

### 4. **Multiple Undo & Redo**
   - **Undo and Redo** buttons to undo or redo the last drawing or erasing action based on a stack of actions
   - Actions are performed continuously (without lifting the finger) for the pencil and eraser tools.

### 5. **Frame Management**
   - **Create a New Frame**: Press the add page button to save the current frame and create a blank canvas for the next frame.
   - **Delete Current Frame**: Press the delete button to delete the current frame
   - **Delete all frames at once**: Ability to clear the whole project by using the 'delete all' button

### 6. **Animation Playback**
   - **Start/Stop Animation Playback**: 
     - When playback starts, all controls are hidden, and only the canvas is visible for viewing the animation.
     - Changes to the artwork during playback are disabled.
     - When playback stops, the user returns to the last frame.

### 7. **Add n random frames**
   - Add many random frames based on user input for n frames, consisting of circles, oval squares, partially open squares, etc.

### 8. **Storyboard**
   - A storyboard-like interface to let the user see all the drawn frames at once and switch to any clicked frame

### 9. **Duplicate frame**
   - Create a new frame by copying all the contents of the current frame to it (useful for creating a slightly different animation frame from the previous one)

### 9. **Playback Speed**
   - Ability to adjust the **playback speed** of the animation

### 10. **Themes**
   - The app is fully compatible with both light and dark themes, ensuring a seamless experience in any environment

### 11. Share
   - Export animation to GIF and share the final animation!

---

## Installation

1. Clone or download the project to your local machine.
2. Open the project in **Android Studio**.
3. Build and run the app on your Android device or emulator.

## Future improvements
1. Ability to pinch and zoom the canvas area for finer control
2. Save multiple drawing projects
3. Smoothen the drawn shapes to a known shape, for e.g. a circle or an alphabet letter
4. Changing the thickness of the pencil and eraser
5. Tool for inserting ready-made geometric figures like arrows, straight lines, rectangles, circles, etc., and interaction with figures: adjusting size by stretching or pinching to zoom, move, rotating.
6. A fill-colour tool for filling colour within the shape boundary
