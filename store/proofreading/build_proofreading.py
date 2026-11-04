import json, re, html, xml.etree.ElementTree as ET
from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill, Border, Side
from openpyxl.worksheet.datavalidation import DataValidation

R = "/home/user/zonmoment"
rows = []  # (section, where, context, text)

# 1. App strings
ctx = {
 "app_name": "App name (do not change)",
}
root = ET.parse(f"{R}/app/src/main/res/values/strings.xml").getroot()
for s in root.findall("string"):
    if s.get("translatable") == "false": continue
    t = "".join(s.itertext()).replace("\\'", "'").replace('\\"', '"')
    note = "Placeholders like %1$s / %2$d are filled in by the app: keep them exactly." if "%" in t else ""
    rows.append(("1. App", s.get("name"), note, t))

# 2. Store listing
L = json.load(open(f"{R}/store/listing-nl.json"))
rows.append(("2. Google Play listing", "title", "Max 30 characters", L["title"]))
rows.append(("2. Google Play listing", "short description", "Max 80 characters", L["shortDescription"]))
for i, para in enumerate([p for p in L["fullDescription"].split("\n\n") if p.strip()], 1):
    rows.append(("2. Google Play listing", f"full description, part {i}", "Max 4000 characters in total", para))

# 3. Screenshot captions
caps = [("screenshot 1", "Elke dag het beste moment", "Was, droog en laad op je eigen zonnestroom"),
        ("screenshot 2", "Zon én stroomprijs per uur", "Met een dynamisch contract kies je ook de goedkoopste uren"),
        ("screenshot 3", "In een minuut ingesteld", "Postcode, panelen en dak. Geen account nodig."),
        ("screenshot 4", "Elke ochtend een seintje", "Je gegevens blijven op je telefoon. Geen reclame, geen tracking.")]
for w, a, b in caps:
    rows.append(("3. Store images", w + " headline", "Big headline above a phone screenshot; keep it short", a))
    rows.append(("3. Store images", w + " subline", "Line under the headline", b))
rows.append(("3. Store images", "feature graphic", "Banner 1024x500, two lines", "Gebruik je eigen zonnestroom.\nElke ochtend het beste moment."))
rows.append(("3. Store images", "notification mock-up", "Shown as 'app name · now' in a notification", "Zonmoment · nu"))

# 4. Website (Dutch parts only)
def blocks(path, stop=None):
    s = open(path, encoding="utf8").read()
    s = re.sub(r"<(script|style|head)[^>]*>.*?</\1>", " ", s, flags=re.S)
    if stop: s = s.split(stop)[0]
    out = []
    for m in re.finditer(r"<(h1|h2|p|li)[^>]*>(.*?)</\1>", s, flags=re.S):
        t = html.unescape(re.sub(r"<[^>]+>", "", m.group(2)))
        t = re.sub(r"\s+", " ", t).strip()
        if t: out.append((m.group(1), t))
    return out
for tag, t in blocks(f"{R}/site/index.html"):
    rows.append(("4. Website", "home page " + tag, "zonmoment.vplusstudio.app", t))
for tag, t in blocks(f"{R}/site/privacy.html", stop='<h2 id="en"'):
    rows.append(("4. Website", "privacy page " + tag, "Privacy policy (Dutch part only)", t))

# 5. Marketing texts (drafts, not yet published)
A1 = [
 "Salderingsregeling stopt op 1 januari 2027: zo haal je meer uit je zonnepanelen",
 "Tot eind 2026 mag je de stroom die je zonnepanelen terugleveren wegstrepen tegen de stroom die je van het net haalt. Vanaf 1 januari 2027 stopt dat. Voor teruggeleverde stroom krijg je dan alleen nog een terugleververgoeding van je energieleverancier, en die is lager dan wat je voor stroom betaalt. Veel leveranciers rekenen daarnaast terugleverkosten.",
 "De oplossing is eenvoudig: gebruik je zonnestroom zelf, op het moment dat de zon schijnt. Elke kilowattuur die je zelf verbruikt, hoef je niet duur in te kopen.",
 "Drie apparaten maken het verschil",
 "Wasmachine en vaatwasser: zet ze aan tussen de late ochtend en de middag, als je panelen het meest leveren.",
 "Droger: draait op veel vermogen; laat hem op het zonnigste uur draaien.",
 "Elektrische auto: laden overdag in plaats van 's nachts kan een groot deel van je teruglevering opvangen.",
 "Maar wanneer schijnt de zon morgen?",
 "Dat verschilt per dag en per dak. Zonmoment rekent het voor je uit: de app combineert de zonnevoorspelling voor jouw postcode met de richting en hoek van je dak, en vertelt je elke ochtend wanneer je je apparaten het beste aanzet. Heb je een dynamisch contract, dan houdt de app ook rekening met de stroomprijs per uur.",
 "Zonmoment is gratis, zonder account en zonder reclame. Download Zonmoment in Google Play",
]
A2 = [
 "Wanneer zet je de wasmachine aan met zonnepanelen?",
 "Kort antwoord: als je panelen meer leveren dan je huis op dat moment verbruikt. Op een zonnige dag is dat meestal tussen 11:00 en 15:00. Op een bewolkte dag kan het venster korter zijn, of later op de dag vallen.",
 "Waarom het uitmaakt",
 "Een wasbeurt kost ongeveer 1 kWh. Draait de machine op je eigen zonnestroom, dan betaal je daar niets voor. Draait hij 's avonds, dan koop je die stroom in, terwijl je overdag dezelfde hoeveelheid voor een lage vergoeding teruglevert.",
 "Het dak bepaalt het moment",
 "Zuid: het meeste rond het middaguur.",
 "Oost-West: een bredere, lagere curve, met een piek in de ochtend en een in de middag.",
 "Oost of West: het beste moment ligt eerder of juist later op de dag.",
 "Laat het uitrekenen",
 "Zonmoment kijkt naar de weersverwachting voor jouw postcode en naar jouw dak, en geeft je elke ochtend het beste tijdvak voor de wasmachine, vaatwasser, droger en auto. Gratis in Google Play",
]
for i, t in enumerate(A1, 1): rows.append(("5. Article: salderen stopt", f"part {i}", "Web article (also please check the facts about salderen)" if i == 2 else "Web article", t))
for i, t in enumerate(A2, 1): rows.append(("6. Article: wasmachine", f"part {i}", "Web article", t))
POST = ["Gratis app die vertelt wanneer je je wasmachine op eigen zonnestroom draait (einde salderen)",
 "Hoi allemaal,",
 "Vanaf 1 januari stopt de salderingsregeling. Daarom heb ik met mijn kleine studio (vplus studio) een Android-app gemaakt: Zonmoment. Je vult je postcode, panelen en dakrichting in, en de app vertelt elke ochtend wanneer je de wasmachine, vaatwasser, droger of auto het beste aanzet. Met een dynamisch contract kijkt hij ook naar de uurprijzen.",
 "Geen account, geen reclame, geen tracking. Alles blijft op je telefoon.",
 "Bronnen: PDOK (postcode), Open-Meteo (zonnevoorspelling), EnergyZero (uurprijzen).",
 "Ik ben benieuwd naar jullie feedback, vooral van mensen met een oost-west-dak of een dynamisch contract: klopt het advies met wat je panelen echt doen?"]
for i, t in enumerate(POST, 1): rows.append(("7. Forum post (Tweakers / Reddit)", f"part {i}", "Informal, honest tone for a Dutch tech forum", t))
rows.append(("8. Facebook post", "post", "Short, informal", "Salderen stopt op 1 januari. Wij hebben een gratis app gemaakt die je elke ochtend vertelt wanneer je je wasmachine, vaatwasser of auto het beste op je eigen zonnestroom laat draaien. Geen account, geen reclame. Feedback is welkom!"))
PRESS = ["Onderwerp: Tip: gratis app plant je wasmachine op eigen zonnestroom (einde salderen)", "Hallo redactie,",
 "Op 1 januari 2027 stopt de salderingsregeling. Voor miljoenen huishoudens met zonnepanelen wordt het dan belangrijk om hun stroom zelf te gebruiken.",
 "Zonmoment is een nieuwe, gratis Android-app die daarbij helpt. Op basis van de postcode, de panelen en het dak vertelt de app elke ochtend wanneer je de wasmachine, vaatwasser, droger of elektrische auto het beste aanzet. Met een dynamisch contract houdt de app ook rekening met de uurprijzen. Er is geen account nodig, er is geen reclame en alle gegevens blijven op de telefoon.",
 "Ik beantwoord graag vragen.", "Met vriendelijke groet,"]
for i, t in enumerate(PRESS, 1): rows.append(("9. Email to tech websites", f"part {i}", "Formal-friendly email to a news editor", t))
INST = ["Onderwerp: Gratis app voor uw klanten na het einde van salderen", "Beste [naam bedrijf],",
 "Veel van uw klanten vragen zich af wat het einde van de salderingsregeling op 1 januari 2027 voor hen betekent. Wij hebben een gratis app gemaakt die hen helpt hun eigen zonnestroom beter te gebruiken: Zonmoment.",
 "De app combineert de zonnevoorspelling voor hun postcode met hun dak en panelen, en vertelt elke ochtend wanneer ze de wasmachine, vaatwasser, droger of auto het beste aanzetten. Geen account, geen reclame, en de gegevens blijven op de telefoon.",
 "U mag de app vrij aanbevelen aan uw klanten, bijvoorbeeld in de opleveringsmail of op uw website. Daar zijn geen kosten of voorwaarden aan verbonden.",
 "Met vriendelijke groet,"]
for i, t in enumerate(INST, 1): rows.append(("10. Email to solar installers", f"part {i}", "Formal (u-form) business email", t))

# Workbook
wb = Workbook()
ins = wb.active; ins.title = "Instructions"
lines = [
 ("Zonmoment — Dutch proofreading", True),
 ("", False),
 ("Zonmoment is an Android app for households in the Netherlands with solar panels. It tells them every morning when to run the washing machine, dishwasher, dryer or electric car on their own solar power, ahead of the end of the 'salderingsregeling' on 1 January 2027.", False),
 ("", False),
 ("What we need", True),
 ("1. Check every text in the sheet 'Texts' for spelling, grammar and natural Dutch. The texts were not written by a native speaker.", False),
 ("2. If a text is fine, choose OK in the column 'Status'.", False),
 ("3. If it needs a change, write the full corrected text in 'Corrected text' and choose 'Changed'. Optionally explain in 'Comment'.", False),
 ("4. Keep the tone: friendly and plain for the app, store and posts (je/jij); polite for the installer email (u).", False),
 ("5. Keep placeholders such as %1$s, %2$d and [naam bedrijf] exactly as they are.", False),
 ("6. Respect the length limits in the 'Context' column (Google Play title max 30, short description max 80 characters).", False),
 ("7. Sections 5–6: if you notice a factual error about salderen or terugleverkosten, please mention it in 'Comment'.", False),
 ("", False),
 ("Do not change brand names: Zonmoment, vplus studio, PDOK, Open-Meteo, EnergyZero, Google Play.", False),
 ("Contact: vplusstudio.app@gmail.com", False),
]
for i, (t, b) in enumerate(lines, 1):
    c = ins.cell(row=i, column=1, value=t); c.font = Font(bold=b, size=14 if i == 1 else 11)
    c.alignment = Alignment(wrap_text=True, vertical="top")
ins.column_dimensions["A"].width = 110

ws = wb.create_sheet("Texts")
head = ["#", "Section", "Where", "Context", "Dutch text", "Corrected text", "Status", "Comment"]
ws.append(head)
for i, (sec, where, c, t) in enumerate(rows, 1):
    ws.append([i, sec, where, c, t, "", "", ""])
widths = [5, 26, 24, 34, 70, 70, 11, 34]
for col, w in zip("ABCDEFGH", widths): ws.column_dimensions[col].width = w
hf = PatternFill("solid", fgColor="F5B400"); thin = Side(style="thin", color="CCCCCC")
for c in ws[1]: c.font = Font(bold=True); c.fill = hf
yellow = PatternFill("solid", fgColor="FFF8E1")
for r in ws.iter_rows(min_row=2):
    for c in r:
        c.alignment = Alignment(wrap_text=True, vertical="top"); c.border = Border(bottom=thin)
    r[5].fill = yellow
dv = DataValidation(type="list", formula1='"OK,Changed"', allow_blank=True)
ws.add_data_validation(dv); dv.add(f"G2:G{len(rows)+1}")
ws.freeze_panes = "E2"; ws.auto_filter.ref = f"A1:H{len(rows)+1}"
out = f"{R}/store/proofreading/zonmoment-nl-proofreading.xlsx"
wb.save(out)
words = sum(len(t.split()) for *_, t in rows)
print(out, len(rows), "rows", words, "words")
