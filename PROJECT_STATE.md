# PROJECT_STATE: Zonmoment

## Current Focus
- **4.11.2026:** Play Console כמעט מוכן: 10/11 משימות הגדרה הושלמו (טפסים, מדיניות פרטיות, פרטי קשר, קטגוריה "House & Home", דף חנות nl-NL עם תמונות — נשמר, **לא נשלח לבדיקה**).
- **7.11.2026: AgeFox אושרה ופומבית ב-Google Play** (production, versionCode 2; דף החנות מחזיר 200). החסם להגשת Zonmoment הוסר.
- **הצעד הבא:** אחרי אישור AgeFox — מרכז שליטה → "השקת האפליקציה": לבדוק אם נדרשת בדיקה סגורה (12 בודקים × 14 יום), ואז "סיכום השינויים לפרסום" → שליחה לבדיקה.
- תוכנית שיווק להולנד (Claude Docs): https://claude.ai/artifact/RavfwjLLRXNZSqt24UhQAG — הצעד הבא בה: בדיקת הולנדית 9–20.11, מאמרים באתר 23–30.11
- קונקטור Cloudflare API (קריאה בלבד) מחובר: אפשר לשלוף תנועה לפי תת-דומיין (GraphQL httpRequestsAdaptiveGroups, עד ~31 יום אחורה, מגבלת קצב)
- בינתיים בלי תלות בגוגל: versionCode 2 עם האייקון החדש, סקריפט העלאה דרך ה-API, בדיקת תנאי Open-Meteo, סמלי NDK.

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
- [x] נבדקו תנאי Open-Meteo (4.11.2026): החינמי רק ללא מנוי/פרסומות. ברגע שיש Pro בתשלום → מנוי API Standard (בערך €29 לחודש, מיליון קריאות; המחיר המדויק בטבלת Stripe באתר). אותו API, רק `customer-api.open-meteo.com` + `&apikey=`
- [x] KNMI נבדק (7.11.2026): HARMONIE-AROME Cy43 p1, רישיון CC BY 4.0 (מותר מסחרית עם קרדיט). מפתח API רשום שמור כ-network secret לסביבה (api.dataplatform.knmi.nl). קובץ לכל ריצה שעתית ~935MB tar (GRIB1 לכל שעת תחזית, ~15.7MB). כולל param 117 = קרינה גלובלית (מצטברת, timerange 4) → קרינה שעתית מהפרש; אין קרינה ישירה/מפוזרת בנפרד → צריך פירוק (Erbs) והטיה לגג. תוכנית גיבוי ל-Open-Meteo; דורש צנרת שרת
- [ ] נשלח מייל ל-info@open-meteo.com (7.11.2026) עם 6 שאלות: חינמי לאפליקציה בלי הכנסה, קריאה ישירה מכל טלפון, מנוי כשיש Pro, proxy+מטמון, נוסח קרדיט, הנחה/חשבונית. מחכים לתשובה
- [ ] לפני Pro: Cloudflare Worker כ-proxy שמחזיק את מפתח Open-Meteo כ-Secret (לא בתוך האפליקציה) ושומר תחזית במטמון לפי אזור
- [ ] קרדיט חובה (CC BY 4.0) בתוך האפליקציה: "Weerdata: Open-Meteo.com" עם קישור
- [x] מדיניות פרטיות מולאה (vplus studio, vplusstudio.app@gmail.com) ונבנה אתר `site/` (index + privacy) עם פריסה ל-Cloudflare → `https://zonmoment.vplusstudio.app/privacy`
- [x] תנאי שימוש NL/EN: `site/voorwaarden.html` → https://zonmoment.vplusstudio.app/voorwaarden (+ `docs/legal/terms-of-service.md`); סטטוס עסק ב-Play: "כן" (5.11.2026)
- [x] האתר נפרס ל-Cloudflare (Worker `zonmoment`, גם `zonmoment.ronmailx.workers.dev`) — 4.11.2026
- [x] מדיניות פרטיות ופרטי קשר הוזנו ב-Play Console (ידנית — ה-API לא תומך בשדה הפרטיות)
- [x] טקסטים לדף החנות בהולנדית: `store/listing-nl.md` / `store/listing-nl.json`
- [ ] ה-API מחזיר 403 על כל שינוי אמיתי בדף החנות (commit ללא שינוי עובר; ב-AgeFox, שכבר הוגשה, זה עובד). כנראה נפתח אחרי ההגשה הראשונה — לבדוק שוב אז
- [x] כל טפסי App content מולאו (4.11.2026), כולל דירוג תוכן PEGI 3 ו-Data safety. קישור מחיקה: `/privacy#verwijderen`

### לפני ההשקה ב-Play
- [x] התמונות הועלו ידנית ל-Play Console ונשמרו כטיוטה (4.11.2026). ה-API חוסם שינויים בדף החנות עד ההגשה הראשונה
- [x] אייקון חדש (שמש זורחת), Feature graphic ו-4 צילומי מסך בהולנדית: `store/images/`. נוצרים מהמסכים האמיתיים: `ZONMOMENT_SCREENSHOTS=1 ROBOLECTRIC_REPO_URL=<mirror> ./gradlew testDebugUnitTest --tests "*StoreScreenshots*"`
- [ ] האייקון החדש נכנס לאפליקציה רק בגרסה הבאה (versionCode 2)
- [x] האפליקציה נוצרה ב-Play Console, ול-`agefox-play@red-grid-513513-t6.iam.gserviceaccount.com` יש הרשאה (בדיקה, דף חנות). אומת מה-API ב-4.11.2026
- [x] workflow "Release build" בונה AAB חתום (versionCode = מספר הריצה), נבדק מקומית עם מפתח זמני
- [x] מפתח העלאה (PKCS12, נוצר ב-Termux עם openssl כי keytool לא עובד שם): נוצר ולשמור 4 Secrets (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `ANDROID_KEYSTORE_PASSWORD`)
- [x] versionCode 1 הועלה ידנית ל-Internal testing (רשימת בודקים "Ron"), הותקן מהחנות ועובד (4.11.2026)
- [ ] סקריפט העלאה דרך ה-API (כמו `store/play_upload.py` בריפו ben-kama-ani-hayom), כדי שהגרסאות הבאות יעלו מכאן
- [ ] סמלי ניפוי באגים ל-native (`ndk.debugSymbolLevel`) כדי לסגור את האזהרה ב-Play
- [ ] Pro: תשלום דרך Google Play Billing (‏€1.99 לחודש או €14.99 לשנה)
- [ ] לבדוק את הטקסטים ההולנדיים עם דובר הולנדית — קובץ למגיה: `store/proofreading/zonmoment-nl-proofreading.xlsx` (132 טקסטים, ~1,650 מילים; נבנה ע"י `build_proofreading.py`)

### אחרי ההשקה
- [ ] מונה חיסכון חודשי
- [ ] וידג'ט
- [ ] גרסה גרמנית ל-Balkonkraftwerk
