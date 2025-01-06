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
  "provider": "http://my.provider.url",
  "username": "1234abcd",
  "password": "dcba4321",
  "my_url": "192.168.2.16",
  "my_port": "8888",
  "my_server_protocol": "http",
  "cfg": {
    "series_categories": [
      "include category_name/^GERMAN/"
    ],
    "vod_categories": [
      "include category_name/^(DE|EN|US|EN)/"
    ],
    "live_categories": [
      "include category_name/^(AT|DE|FOR ADULTS)([|] |$)/",
      "exclude category_name/(?i)(sport|dazn|rtl[+] ppv|prime ppv|formula 1 ppv|dyn ppv|discovery[+] ppv|bundesliga)/"
    ],
    "live_streams": [
      "replace name/\\s+$//",
      "exclude name/(UHD|ᵁᴴᴰ|DAZN)/",
      "exclude name/^###/",
      "replace name/\\s+(RAW|4K|\\(720P\\)|HEVC|\\(SAT\\)|\\(LOW\\s*BIT\\))//",
      "exclude name/(?i)sport/",
      "exclude name/\\bORF ?2 ?[A-Z].*/",
      "capitalize name/\\b(SKY(\\s\\b[A-Z]{3,}\\b)+)/",
      "replace name/\\b(Sky.*HD)/$1_/",
      "replace name/\\bHD$//",
      "replace name/\\bHD_$/HD/",
      "replace name/\\bORF 3/ORF III/",
      "replace epg_channel_id/.+//",
      "replace name/^PLAY[+]:\\s+/AT: /",
      "replace name/ +ADIO / RADIO /",
      "replace name/\\b(RTL|ATV)\\s?2/$1 II/",
      "replace name/\\bSAT\\s1/SAT.1/",
      "replace name/\\bWARNERTV\\b/WARNER TV/"
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
