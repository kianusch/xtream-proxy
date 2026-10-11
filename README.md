# xtream-proxy

**Easily manipulate IPTV XTREAM playlist for a customized viewing experience.**

---

## 📢 Important Note

This tool requires a valid IPTV stream subscription. Ensure your subscription is active before using `xtream-proxy`.

---

## 📝 Overview

`xtream-proxy` connects to your IPTV provider using XTREAM credentials and retrieves categories and streams for:

- **Live TV**
- **Video-on-Demand (VOD)**
- **Series**

### Key Features:

- Modify channel or group titles.
- Include or exclude specific groups/channels.
- Generate a cleaned and streamlined playlist compatible with players that use XTREAM API calls.

### How It Works:

- Any unsupported API call is seamlessly redirected to your streaming provider, ensuring uninterrupted service.

---

## 🚀 Usage

1. Obtain valid XTREAM credentials from your IPTV provider.
2. Configure `xtream-proxy` with your credentials and settings.
3. Define filtering and manipulation rules in the configuration file (`xtream.json`).
4. Use the generated playlist in your IPTV player.

Run this Programm on your PC ...

java -cp ./jm3u-1.0-SNAPSHOT.jar:./* my.xtream.Main

In your IPTV app - replace the URL for your provider with the URL/PORT from your proxy, but keep the credentials from your provider.

e.g. http://my.provider.url becomes http://192.168.2.16:8888

Enjoy a more personalized IPTV experience! 🎉

---

## ⚙️ Configuration Example (`xtream.json`)

Below is an example configuration file for `xtream-proxy`.  
> **Note:** To retain the EPG link from your provider, remove the `epg` line from the configuration.

```json
{
  "provider": "https://provider.foo.bar",
  "keep_uplink": true,
  "me": "http://192.168.2.16:25461",
  "cfg": {
    "series_categories": [
      "include category_name/^.*/"
    ],
    "vod_categories": [
      "include category_name/^.*/"
    ],
    "live_categories": [
      "include category_name/^(CH|AT|DE|IR|FOR ADULTS)([|] |$)/",
      "exclude category_name/(?i)((sport|dazn|dyn|staige|prime|formula 1)|((rtl|disney|discovery)[+]) ppv|bundesliga|soccer|football|inf|sfl)/"
    ],
    "live_streams": [
      "noop replace ~category_name/^(.*)[|].*$/$1/",
      "replace name/\\s+$//",
      "exclude name/(UHD|ᵁᴴᴰ|DAZN)/",
      "exclude name/(?i)sport/",
      "exclude name/^###/",
      "replace epg_channel_id/.+//",
      "replace name/^..:\\s+//",
      "replace name/^(?:ADULT|JOYN|SKYGO|WOW|PRIME|PLAY[+]):\\s+//",
      "replace name/\\s+(ⱽᴵᴾ|ᴳᴼᴸᴰ|ᴿᴬᵂ|RAW|4K|\\(720P\\)|HEVC|\\(SAT\\)|\\(LOW\\s*BIT\\))//",
      "replace name/\\b(SKY.*HD)/$1_/",
      "replace name/ᴴᴰ ◉/HD_/",
      "replace name/\\bHD$//",
      "replace name/\\bHD_$/HD/",
      "replace name/^(ZDF)\\B/$1 /",
      "replace name/^ORF\\sEINS/ORF1/",
      "replace name/^ORF\\sIII/ORF3/",
      "replace name/^ORF\\s(1|2|3)\\b/ORF$1/",
      "replace name/^(ORF)2\\s(.*)/$1 $2/",
      "replace name/^((RTL|VOX)\\B|SAT)[ .]?/$1 /",
      "replace name/^(RTL) 2/$1 ZWEI/",
      "replace name/^(KABEL) 1/$1 EINS/",
      "replace name/^(PULS|ATV)\\s+/$1/",
      "replace name/^PROSIEBEN/PRO SIEBEN/",
      "replace name/^NAT GEO$/NATIONAL GEOGRAPHIC/",
      "replace name/^([\\w][\\w]+)[,-.]?TV\\b/$1 TV/",
      "replace name/^(WARNER.*)\\sHD$/$1/",
      "replace name/\\b(CINEMA )(HIGHLIGHTS?|SPECIAL)(\\sHD)?$/$1BLOCKBUSTER$3/",
      "replace name/(SUPER)\\s(RTL)/$2 $1/",
      "replace name/(ᴴᴰ|ᶠ)//",
      "replace name/[\\s-]+$//",
      "replace name/\\b(PREMIERE)N( HD)?$/$1$2/",
      "replace name/NTRO/NITRO/",
      "replace name/(CRIME) ?[+] ?(INVESTIGATION)/$1 & $2/",
      "replace name/^(WDR|NDR|MDR|RBB|SWR|BR FERNSEHEN) HD/$1/",
      "replace name/\\b(SKY CINEMA) FAMILY\\b/$1 FEELGOOD/",
      "set ~category_name/^(DE|AT)[|] / stream_icon/https://raw.githubusercontent.com/tv-logo/tv-logos/refs/heads/main/countries/germany/${name}-de.png/",
      "set name/^(ORF|ATV|PULS|TIROL|W24|SERVUS|K19|R9|KRONE|KRONEHIT|KURIER|K-TV|OKTO|(COMEDY CENTRAL|SIXX) AUSTRIA)/ stream_icon/https://raw.githubusercontent.com/tv-logo/tv-logos/refs/heads/main/countries/austria/${name}-at.png/",
      "set name/(?i)^SKY.*HD$/ stream_icon/https://raw.githubusercontent.com/tv-logo/tv-logos/refs/heads/main/countries/germany/hd/${}-de.png/",
      "noop set name/(?i)^RTL / stream_icon/https://raw.githubusercontent.com/tv-logo/tv-logos/refs/heads/main/countries/germany/${}-de.png/",
      "set name/^(?i)SKY.*(NATURE|DOCUMENTARIES|CLASSICS)/ stream_icon/https://raw.githubusercontent.com/tv-logo/tv-logos/refs/heads/main/countries/germany/${}-alt-de.png/",
      "replace stream_icon/-hd-alt/-alt/",
      "replace stream_icon/-tv-hd/-tv/",
      "replace stream_icon/[&]/and/",
      "replace stream_icon/[+]/plus/",
      "replace stream_icon/rtl-(nitro-)/$1/",
      "replace stream_icon/-austria-de[.]png$/-de.png/",
      "replace stream_icon/hd.(sky-replay)-hd/$1/",
      "replace stream_icon/\\b(sky-cinema)-feelgood\\b/$1-family/",
      "replace stream_icon/\\b(sky-cinema)-blockbuster\\b/$1-highlights/",
      "replace stream_icon/-de(-de)/$1/",
      "replace stream_icon/\\b(orf2)\\w\\w?-at/$1-at/",
      "replace stream_icon/crime-and-investigation-hd-de.png/crime-and-investigation-de.png/",
      "replace stream_icon/(orf-)n.(-at)/$1niederosterreich$2/",
      "deumlaut stream_icon",

      "noop"
    ]
  }
}
```

---

## 🛠 Features Explained

### Category Manipulation:
- **Include/Exclude:** Filter categories by name patterns using regex.
  
### Stream Manipulation:
- **Modify Titles:** Replace, capitalize, or exclude stream titles with precise regex rules.
- **EPG Adjustments:** Clear or modify EPG channel IDs as needed.

---

## 🧩 Contributing

Contributions are welcome! Please submit a pull request or open an issue to improve this project.

---
