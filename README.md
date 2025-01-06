# xtream-proxy

Manipulate IPTV XTREAM streams with ease.

## Important Note
This program will not work unless you have a valid IPTV stream subscription. Ensure your subscription is active before using this tool.

## Overview
`xtream-proxy` connects to your IPTV streaming provider using your XTREAM credentials. It retrieves categories and streams for live TV, video-on-demand (VOD), and series. Once retrieved, it allows you to:

- Modify channel or group titles.
- Filter groups/channels by including or excluding specific ones.
- Generate a streamlined, cleaned-up playlist for your player.

This cleaned playlist is compatible with players that utilize XTREAM API calls.

## Functionality
Any API call not explicitly handled by `xtream-proxy` will be redirected to your streaming provider seamlessly, ensuring uninterrupted service.

## Usage
1. Ensure you have valid XTREAM credentials from your IPTV provider.
2. Configure the proxy to connect to your provider.
3. Define your desired filtering and manipulation rules.
4. Use the generated playlist in your IPTV player.

Enjoy a simplified and customized IPTV experience!

## Example Config File (xtream.json)
```json
{
  "provider": "http://my.provider.url",
  "username": "1234abcd",
  "password": "dcba4321",
  "#epg": "http://192.168.2.16:3000/guide.xml",
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

