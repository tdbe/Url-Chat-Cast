<img width="640" height="340" alt="tv_banner" src="https://github.com/user-attachments/assets/c2635d13-faee-42ee-ad49-9b694a8d4a22" />

# Url Chat Cast

Cast links from any device, using your own chat apps, to any Android 11+ device or TV that runs this app. Url Chat Cast forwards the URL when it gets the command via any whitelisted app's public OS notification. A basic functionality that the entire apps industry hasn't managed to generically provide us in all its decades.

Options: choice popup, auto-open, web browser vs builtin app, no-defaults OS-choice. 

No servers, no transmission protocols, no redirects, no network or internet, no narrow / proprietary ecosystems.

This repo is a basic kotlin gradle project, tweaked, tested, iterated, read-through, architected by [me](https://github.com/tdbe) but slopped using Qwen 3.8 Flash Next 180B a6B Q4. Provided openly for free as-is, since nobody else did, in 2026, wtf.

## Trigger

It activates on notification events. The notification's message needs to come from an app you've selected in the dropdown, and contain a keyword pattern e.g. `please open htt` and will then extract the url from the whole text e.g. "please open https://www.duckduckgo.com". Note that any text you include after the url will be considered part of the url.

The app expects one url containing "://" to be found after the trigger text you enter here, or the url to be (partially) in this trigger text. Leave the field blank to open any received url.

## Install

Get the latest Release .apk file from the releases section and put it on your device / TV and tap it to install. You can also use the Obtainium / ObtainX "store" to track it and install from there. (but there's not much need for updates for such base and well defined functionality)

## Build It Yourself

Open this folder in Android Studio, have some target sdk installed e.g. 36 (which includes 30 (android 11)) (via the gear icon > SDK Manager), and from top bar menu click Build > Generate App Bundles or APKs > Generate APKs.

## Permissions and queries

In case you don't believe that I'm not doing anything funny e.g. wrapping urls like twitter does, and you can't read code, you can install [Untrack Me](https://f-droid.org/packages/app.fedilab.nitterizeme/) as a browser to consume the urls forwarded by Url Chat Cast, and it'll pause and show you there's no redirect business going on.

Like I listed above, there's no unnecessary permissions, nothing used other than the OS API to read the non-private class of notifications from Android's notification drawer.

The app responds to notification received events, but to trigger the android system popup and/or request to open the links (which is a Foreground action), I'm also using the Overlay (`SYSTEM_ALERT_WINDOW`) permission. The only Android 11+ and also TV-compatible options to auto-start a Foreground action, are either: this Overlay alert, the Accessibility Service apps (the kind that read your screen), or the ugly Leanback Launcher Recommendation Cards (TV-only, depend on stock launcher).

`app/src/main/AndroidManifest.xml` shows we only use:
```
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<!-- The SpecialUse is the Type of foreground service. It's a catch-all designation in Android 14 and higher for valid long-running foreground tasks that do not fit into any of the standard types (such as location, mediaPlayback, camera, or dataSync)-->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
	
<intent>
	<action android:name="android.intent.action.VIEW" />
	<data android:scheme="https" />
</intent>
	
android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"	
```
