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
3. Define filtering and manipulation rules in the configuration file (`xtream.properties`).
4. Use the generated playlist in your IPTV player.

Run this Programm on your PC ...

java -cp ./jm3u-1.0-SNAPSHOT.jar:./* my.xtream.Main

In your IPTV app - replace the URL for your provider with the URL/PORT from your proxy, but keep the credentials from your provider.

e.g. http://my.provider.url becomes http://192.168.2.16:8888

Enjoy a more personalized IPTV experience! 🎉

---

## ⚙️ Configuration Example (`xtream.properties`)
There are two configuration files: the base configuration (`xtream.properties`) and a filter file (`xtream.filter`).

Below is an example configuration file for `xtream-proxy`.  
> **Note:** To retain the EPG link from your provider, remove the `epg` line from the configuration.

```properties
epg=https://epgshare01.online/epgshare01/epg_ripper_DE1.xml.gz
epg.de=https://epgshare01.online/epgshare01/epg_ripper_DE1.xml.gz
epg.at=https://epgshare01.online/epgshare01/epg_ripper_AT1.xml.gz
provider=https://xxx.foo.bar
keep_uplink=true
me=http://192.168.1.1:25461
```

```text
series_categories
  category_name
   i/^.*/
vod_categories
  category_name
   i/^.*/
live_categories
  category_name
   i/^(DE)([|] |$)/
   e/(?i)((sport|dazn|dyn|staige|prime|formula 1)|((rtl|disney|discovery)[+]) ppv|bundesliga|soccer|football|inf|sfl)/
live_streams
  epg_channel_id
   s/.+//
  name
   s/\s+$//
   e/(UHD|ᵁᴴᴰ|DAZN)/
   e/(?i)sport/
   e/^###/
   s/^..:\s+//
   s/^(?:ADULT|JOYN|SKYGO|WOW|PRIME|PLAY[+]):\s+//
   s/\s+(ⱽᴵᴾ|ᴳᴼᴸᴰ|ᴿᴬᵂ|RAW|4K|\(720P\)|HEVC|\(SAT\)|\(LOW\s*BIT\))//
   s/ *(?:ᴴᴰ|ᶠ|ᴴᴰ ◉|◉)//
   s/^((?:SKY|COMEDY|SERVUS).*HD)/$1_/
   s/^((?:UNIVERSAL|RED).*)/$1_/
   s/^(ZDF|ORF)\B/$1 /
   s/^(ORF)\sEINS/$1 1/
   s/^(ORF)\sIII/$1 3/
   s/^(ORF\s.+)(?:\sHD)$/$1/
   s/^(ORF)(?:\s2\s?)(.+)$/$1 $2/
   s/^((RTL|VOX)\B|SAT)[ .]?/$1 /
   s/^(RTL) 2/$1 ZWEI/
   s/^(KABEL) 1/$1 EINS/
   s/^(PULS|ATV)\s+/$1/
   s/^PROSIEBEN/PRO SIEBEN/
   s/^NAT GEO$/NATIONAL GEOGRAPHIC/
   s/^(NAT)IONAL (GEO)GRAPHIC (WILD)$/$1 $2 $3/
   s/^([\w][\w]+)[,-.]?TV\b/$1 TV/
   // replace name/^(WARNER.*)\sHD$/$1/
   s/(?:\s|\B)HD(\s|$)/$1/
   s/\bHD_$/HD/
   s/\b(CINEMA )(HIGHLIGHTS?|SPECIAL)(\sHD)?$/$1BLOCKBUSTER$3/
   s/(SUPER)\s(RTL)/$2 $1/
   s/\b(PREMIERE)N( HD)?$/$1$2/
   s/NTRO/NITRO/
   s/(CRIME) ?[+] ?(INVESTIGATION)/$1 & $2/
   // replace name/^(WDR|NDR|MDR|RBB|SWR|BR FERNSEHEN) HD/$1/
   s/\b(SKY CINEMA) FAMILY\b/$1 FEELGOOD/
   s/^(GEO) TELEVISION$/$1/
   s/^(OE)\s(24)\sTV$/$1$2/
   // replace name/^(OE24|KINOWELT) TV$/$1/
   s/\s+TV$//
   s/^(N)-(TV)$/NTV/
   s/^(RED)(BULL)(TV_)$/$1 $2 $3/
   s/^(HITRADIO O)E(3)$/$1$2/
   s/^(HISTORY) .*/$1/
   s/^(SERVUS).*/$1 TV/
   s/\B_$//
  // direct_source
   // =${uplink}/live/${username}/${password}/${stream_id}.ts
  stream_icon
   s/~category_name/^(DE|AT)[|] /https://raw.githubusercontent.com/tv-logo/tv-logos/refs/heads/main/countries/germany/${name}-de.png/
   s/name/^(?i)SKY.*(NATURE|DOCUMENTARIES|CLASSICS)/https://raw.githubusercontent.com/tv-logo/tv-logos/refs/heads/main/countries/germany/${}-alt-de.png/
   s/-hd-alt/-alt/
   s/((?<!hd)\/[a-z0-9][^\/]*-hd-de[.]png)/\/hd$1/
   s/[&]/and/
   s/[+]/plus/
   s/rtl-(nitro-)/$1/
   s/-austria-de[.]png$/-de.png/
   s/hd.(sky-replay)-hd/$1/
   s/\b(sky-cinema)-feelgood\b/$1-family/
   s/\b(sky-cinema)-blockbuster\b/$1-highlights/
   s/-de(-de)/$1/
   // replace stream_icon/crime-and-investigation-hd-de.png/crime-and-investigation-de.png/
   s/(orf-)n..?(-at)/$1niederosterreich$2/
   s/(orf)(?:-)([123]-at)/$1$2/
   deumlaut
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
