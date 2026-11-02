# Demo video script (Part 2)

Record the app on your **phone** with a voice-over. Aim for 5–7 minutes. Tick each item as you show it.
Have the MockAPI dashboard open in a browser tab on your computer so you can cut to it.

| # | Show on screen | Say (voice-over, in your own words) |
|---|---|---|
| 1 | Splash → Login screen | Introduce Varsivo, your name/team and the problem it solves for Grade 12 learners. |
| 2 | **Register** with a bad password, then a valid one | "Validation stops weak passwords and mismatches. The password is salted and hashed with PBKDF2 before it is saved." |
| 3 | **MockAPI → users** table | Point at the new row: `salt` and `passwordHash`, **no plain password**. |
| 4 | **Log in** (wrong password first, then correct) | Show the error message, then success → Home. |
| 5 | **Settings** → change name, language, notifications → Save | Then **Change password**, log out, log in with the new password. Show the `users` row updated in MockAPI. |
| 6 | **Bursaries** → search → View more → open details | "These come from our hosted REST API – `GET /bursaries`." Show the MockAPI `bursaries` table. |
| 7 | **Universities** → search → Apply now → submit form | Try an invalid email first to show validation. |
| 8 | **Applications tracker** | Tap the status chip, delete one. Show "Synced" and the MockAPI `applications` table. |
| 9 | **Map** (View on map) | "Nominatim REST API gives the coordinates; MapLibre draws the map." |
| 10 | **Explore** → APS calculator | Enter marks, show matched institutions; enter 150 to show the validation message. |
| 11 | **UniAssist** | Timetable and deadlines. |
| 12 | **GitHub**: repo, README, Actions tab (green run), Logcat | "Automated tests run on every push." Show a few log lines in Logcat. |

Tips: turn on airplane mode for one application to show **"Offline – will sync…"**, then turn it off and reopen the
tracker so it syncs.
