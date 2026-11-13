# Aletheia

A private reading room for Android. You write. She answers through your OpenRouter account. Her messages stay on the device, and every one of them can be copied out: onto the clipboard, and into a folio of passages you lifted.

The OpenRouter key is typed into the app and kept in the device keystore. It is sent only as a bearer token to `https://openrouter.ai`. Nothing in this repository contains a key.

## Languages

Kotlin is the room: the archive, the page, and the copy gesture.

Java is the wire. `wire/` builds the chat request, reads the event stream, and turns OpenRouter errors into a sentence.

## Run

Open the project in Android Studio, or from this directory:

```
./gradlew assembleDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk`. On first launch, add an OpenRouter key under Account. The model field accepts `openrouter/auto` or any `provider/model` id from your account. Her language is chosen there too; if you write to her in another language, she answers in that one.

Copy sits on each of her messages. The words are also selectable. More → Copy her replies copies the whole page. Discarding a reading removes it from the archive and leaves the folio alone.

Newsreader and Outfit are used under the SIL Open Font License. See `third_party/fonts/OFL.txt`.
