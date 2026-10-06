# MonoClock

A clock app for the Mudita Kompakt, built with MMD (Mudita Mindful Design). It rings alarms, counts down a timer, and runs a stopwatch. Everything is black and white and nothing animates, so it stays calm on the e-ink screen.

The app leans on Android's own machinery wherever it can: alarms and the timer are scheduled through the system alarm clock service (they ring on time even when the phone sleeps, and the next alarm shows in the status bar), the sound is your system default alarm sound at the alarm volume, and other apps can set alarms or timers here through the standard Android clock intents.

## Install

Download the latest APK from the [releases page](../../releases) and sideload it,
or build from source:

    ./gradlew installDebug

## How it works

Alarm times follow your system 12/24-hour setting. Alarms can repeat on chosen weekdays. An alarm without repeat days rings once and switches itself off. When an alarm rings, the screen wakes with Dismiss and Snooze buttons; snooze waits ten minutes. Ringing stops on its own after ten minutes if you don't react. Alarms survive a reboot.

The timer counts down any duration up to 99 hours: build it up with the +5/+10/+30 minute and +1 hour buttons, fine-tune by the minute, and it rings like an alarm when it finishes. It keeps running when you leave the app or the phone sleeps.

The stopwatch counts whole seconds (the e-ink screen is too slow for anything faster) and supports laps. It keeps counting while the app is closed, but resets on reboot.

## Privacy

No network access, no analytics, nothing leaves the phone. Alarms and timer state live in the app's local storage. The app asks for notification permission because Android requires a notification to ring an alarm reliably.

## Structure

- `MainActivity.kt` - the four tabs and the standard clock intents
- `ClockViewModel.kt` - alarm, timer, and stopwatch state
- `AlarmScheduler.kt` - scheduling through the system alarm clock service
- `AlarmService.kt` - plays the sound and vibrates while ringing
- `RingActivity.kt` - the full-screen Dismiss/Snooze screen
- `AlarmReceiver.kt`, `BootReceiver.kt` - alarm delivery and rescheduling after reboot
- `data/` - the alarm model and local storage
- `ui/` - one file per tab, plus shared pieces like the time picker

## Support

If you find this app useful, consider [sponsoring me](https://github.com/sponsors/berendsliedrecht).

## License

[MIT](LICENSE)
