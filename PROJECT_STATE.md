# PROJECT_STATE: Zonmoment

## Current Focus
ה-MVP מוזג ל-`main`, הותקן בטלפון ועובד (אושר על ידי המשתמש ב-4.11.2026). ה-APK זמין ב-Releases → latest.
**הצעד הבא:** לבחור את השלב הבא: בדיקה עם משתמשים הולנדים, Pro בתשלום, או הכנה לחנות.

## החלטות
- **אפליקציית אנדרואיד נייטיב** (Kotlin + Jetpack Compose), כדי שההתראה של הבוקר תעבוד בלי שרת (WorkManager).
- **שפת הממשק: הולנדית** כברירת מחדל, ואנגלית ב-`values-en`. זו חריגה מכלל "עברית ו-RTL" שב-CLAUDE.md, כי קהל
  היעד הולנדי. תמיכת RTL נשארת פעילה במניפסט (`supportsRtl`).
- **Application ID:** ‏`app.vplusstudio.zonmoment`, לפי הדומיין vplusstudio.app. **אי אפשר לשנות אותו אחרי
  הפרסום ב-Play.**
- **מקורות נתונים בלי מפתח API:** PDOK, ‏Open-Meteo ו-EnergyZero.
- **החשבון ב-Play Console הוא ארגוני**, כך שאין חובת closed test של 12 בודקים ו-14 יום.
- **מדיניות הפרטיות בהולנדית ובאנגלית**, ולא בעברית כפי ש-CLAUDE.md מגדיר, כי היא מיועדת למשתמשים הולנדים
  ול-Google Play.

## משימות
### MVP
- [x] פרויקט Gradle (AGP 8.13.2, ‏Kotlin 2.2.21, ‏compileSdk/targetSdk 36, ‏minSdk 26)
- [x] מודל סולארי וחישוב החלון הטוב ביותר, לחוזה קבוע ולחוזה דינמי
- [x] מפענחים ל-PDOK, ‏Open-Meteo (כולל הסטה לשעה הקודמת) ו-EnergyZero (ממוצע לשעה)
- [x] מסך הגדרות: מיקוד, פאנלים, Wp, כיוון גג, סוג חוזה, מכשירים
- [x] מסך בית: המלצות להיום ולמחר, וגרף ייצור ומחיר
- [x] התראה יומית ב-07:30 (WorkManager, ‏POST_NOTIFICATIONS)
- [x] בדיקות יחידה (9) ו-Lint נקי משגיאות
- [x] GitHub Actions: בדיקות, Lint ו-APK. מ-`main` מתפרסם גם release בשם `latest`
- [x] מדיניות פרטיות (NL/EN)
- [x] **בדיקה בטלפון אמיתי**: מותקן ועובד
- [x] לאמת את פורמט ה-API העדכני של EnergyZero (‏`interval=4`, שדות `Prices[].price` ו-`readingDate`) — אומת מול השרתים האמיתיים ב-4.11.2026, כולל PDOK ו-Open-Meteo
- [x] בדיקה חיה `LiveApiTest` (רצה רק עם `ZONMOMENT_LIVE=1`) ו-workflow שבועי "Live API check"
- [x] הודעה כשמחירי מחר עוד לא פורסמו (מתפרסמים בסביבות 14:00)
- [ ] לבדוק את תנאי השימוש המסחרי של Open-Meteo, ובמידת הצורך לעבור למנוי בתשלום או ל-Forecast.Solar
- [ ] לפרסם את מדיניות הפרטיות בכתובת ציבורית (GitHub Pages או Cloudflare)

### לפני ההשקה ב-Play
- [ ] אייקון סופי וצילומי מסך לחנות בהולנדית
- [x] האפליקציה נוצרה ב-Play Console, ול-`agefox-play@red-grid-513513-t6.iam.gserviceaccount.com` יש הרשאה (בדיקה, דף חנות). אומת מה-API ב-4.11.2026
- [x] workflow "Release build" בונה AAB חתום (versionCode = מספר הריצה), נבדק מקומית עם מפתח זמני
- [ ] מפתח העלאה: ליצור ב-Termux ולשמור 4 Secrets (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `ANDROID_KEYSTORE_PASSWORD`)
- [ ] להעלות ידנית את ה-AAB הראשון ל-Internal testing (חובה באפליקציה חדשה)
- [ ] Pro: תשלום דרך Google Play Billing (‏€1.99 לחודש או €14.99 לשנה)
- [ ] לבדוק את הטקסטים ההולנדיים עם דובר הולנדית

### אחרי ההשקה
- [ ] מונה חיסכון חודשי
- [ ] וידג'ט
- [ ] גרסה גרמנית ל-Balkonkraftwerk
