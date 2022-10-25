# MorseBuddy: Decode Morse Code
MorseBuddy is a simple android application that can decode Morse signals using the phone camera. It detects light transmitted from a distant signal lamp and processes the recorded morse signal to obtain morse code. Finally, it uses the Morse code table to translate it into readable text.

The camera system uses the [CameraX](https://developer.android.com/training/camerax) library, which provides a consistent, easy-to-use API and employs a simple use case-based approach that is also lifecycle-aware. It also resolves device compatibility issues.

## Procedure
- Generate samples by processing camera frames from the camera device.
- Record samples at a fixed rate to construct a complete signal.
- Calculate the duration of dots, dash, and spaces in the record using some techniques.
- Resolve Morse code and try to translate it into readable text.

## Screenshots
<div>
<img src="https://user-images.githubusercontent.com/63910661/197873114-7a5a794a-b137-4923-9042-a266bf2afd9b.png" alt="preview_1" width="240px" />
<img src="https://user-images.githubusercontent.com/63910661/197873643-feb9b2c4-e90b-46ad-b910-d23f087e5373.png" alt="preview_2" width="240px" />
</div>

## About Morse Code
- [https://en.wikipedia.org/wiki/Morse_code](https://en.wikipedia.org/wiki/Morse_code)
- [https://morsecode.world/international/translator.html](https://morsecode.world/international/translator.html)
