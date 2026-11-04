# Zonmoment ☀️

אפליקציית אנדרואיד לבעלי פאנלים סולאריים בהולנד. היא אומרת **מתי כדאי להפעיל מכונת כביסה, מדיח, מייבש או
טעינת רכב חשמלי**, כדי להשתמש בחשמל מהגג במקום להחזיר אותו לרשת. הצורך הזה גדל מאוד מ-1.1.2027, כשהקיזוז
הנטו בהולנד (*salderingsregeling*) מסתיים.

ממשק האפליקציה בהולנדית, עם גרסה באנגלית. הקוד והתיעוד באנגלית ובעברית.

## איך היא עובדת
| שלב | מקור | הערות |
|-----|------|-------|
| מיקוד → קואורדינטות | [PDOK Locatieserver](https://api.pdok.nl/bzk/locatieserver/search/v3_1/free) (ממשלת הולנד) | פעם אחת, בהגדרה |
| קרינת שמש על הגג, לכל שעה | [Open-Meteo](https://open-meteo.com/) `global_tilted_irradiance` | לפי זווית וכיוון. גג מזרח-מערב מחושב כשני חצאים |
| מחירי חשמל לכל שעה | [EnergyZero](https://api.energyzero.nl/v1/energyprices) | רק בחוזה דינמי. מחיר שוק כולל מע"מ |

החישוב: ‎`kW = kWp × קרינה / 1000 × 0.85`‎. לכל מכשיר נבחר החלון עם הכי הרבה חשמל מהגג. בחוזה דינמי נבחר
החלון עם העלות הנמוכה ביותר של החשמל שעדיין מגיע מהרשת. ההמלצות הן רק בין 07:00 ל-22:00.

אין שרת ואין חשבון. כל ההגדרות נשמרות בטלפון. ההתראה של הבוקר (07:30) נוצרת בטלפון עצמו בעזרת WorkManager.

## מבנה הקוד
```
app/src/main/java/app/vplusstudio/zonmoment/
├── domain/   Models, SolarModel, Planner: לוגיקה טהורה, עם בדיקות יחידה
├── data/     Parsers, ForecastRepository (HTTP), SettingsStore
├── ui/       Compose: SetupScreen, HomeScreen, MainViewModel, Theme
└── work/     DailyAdvice: ההתראה היומית
```

## בנייה
- **מהטלפון:** כל push מריץ את GitHub Actions (בדיקות, Lint וקובץ APK). ב-`main` ה-APK מתפרסם גם תחת
  **Releases → latest**, ומשם מורידים ומתקינים אותו.
- **במחשב:** ‏`./gradlew testDebugUnitTest assembleDebug`. צריך JDK 17 ומעלה ו-Android SDK 36.

## מסמכים
- [`PROJECT_STATE.md`](PROJECT_STATE.md): סטטוס ומשימות
- [`docs/legal/privacy-policy.md`](docs/legal/privacy-policy.md): מדיניות פרטיות בהולנדית ובאנגלית, עבור Google Play
