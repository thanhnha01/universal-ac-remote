# IRremoteESP8266 Detailed A/C protocol catalog

Generated from `SupportedProtocols.md` at pinned commit `1e2f0f3ef0a93cbf2a8ddb2e95130f8f4c584b3f`. This is a support inventory, not a claim that all listed models have been validated on physical hardware.
The upstream document is generated metadata; its embedded source links may point at a moving branch. Use the SHA above and `app/src/main/cpp/UPSTREAM_MANIFEST.txt` for provenance.

Detailed A/C rows: 85; distinct protocol families: 42.

## SUPPORTED_GENERIC

These family entries are enabled in this APK and go through the single JNI `encodeAc(protocolId, modelId, state)` path. Exact upstream IDs/model IDs and temp limits are in `ProtocolRegistry.kt`; carrier is collected from upstream `IRsend::enableIROut`.

- Daikin: `daikin` → `DAIKIN`
- Fujitsu: `fujitsu_ac` → `FUJITSU_AC`
- Gree: `gree` → `GREE`
- LG: `lg` → `LG`
- Midea: `midea` → `MIDEA`
- Mitsubishi: `mitsubishi_ac` → `MITSUBISHI_AC`
- Panasonic: `panasonic_ac` → `PANASONIC_AC`
- Samsung: `samsung_ac` → `SAMSUNG_AC`

## SUPPORTED_WITH_SPECIAL_HANDLING

Detailed A/C support exists in the upstream catalog, but the current APK has not enabled these senders. Admission requires source closure/build flags, verified model selection and per-protocol capability/golden vectors. `IRac::isProtocolSupported()` is the authoritative compile-time generic-dispatch allowlist.

- Airton
- Airwell
- Amcor
- Argo
- Bosch
- Carrier
- Coolix
- Corona
- Delonghi
- Ecoclim
- Electra
- Eurom
- Goodweather
- Haier
- Hitachi
- Kelon
- Kelvinator
- Mirage
- MitsubishiHeavy
- Neoclima
- Rhoss
- Sanyo
- Sharp
- Tcl
- Technibel
- Teco
- Toshiba
- Transcold
- Trotec
- Truma
- Vestel
- Voltas
- Whirlpool
- York

Potentially generic through `IRac` after metadata and tests: DAIKIN128, DAIKIN152, DAIKIN160, DAIKIN176, DAIKIN2, DAIKIN216, DAIKIN312, DAIKIN64, HAIER_AC, HAIER_AC160, HAIER_AC176, HAIER_AC_YRW02, HITACHI_AC1, HITACHI_AC264, HITACHI_AC296, HITACHI_AC344, HITACHI_AC424, KELON, LG2, MITSUBISHI112, MITSUBISHI136, PANASONIC_AC32, SANYO_AC, TROTEC, TROTEC_3550

## NOT_YET_SUPPORTED

Protocol variants mentioned by detailed A/C device rows but absent from `IRac::isProtocolSupported()` at the pinned commit cannot use the current common `IRac::sendAc` contract. They need another upstream path or an explicit adapter; the generator never promotes them automatically.

- `CARRIER_AC128`
- `CARRIER_AC84`
- `DAIKIN200`
- `HITACHI_AC3`
- `KELON168`
- `SANYO_AC152`
- `TCL96AC`

## Source rows

| Upstream family | Brands | Models/devices (upstream text) | A/C model variants |
|---|---|---|---|
| Airton | Airton | RD1A1 remote; SMVH09B-2A2A3NH ref. 409730 A/C |  |
| Airwell | Airwell | DLS 21 DCI R410 AW A/C; RC04 remote; RC08W remote |  |
| Amcor | Amcor | ADR-853H A/C; TAC-444 remote; TAC-495 remote |  |
| Argo | Argo | Ulisse 13 DCI Mobile Split A/C [WREM2 remote]; Ulisse Eco Mobile Split A/C (Wifi) [WREM3 remote] | SAC_WREM2; SAC_WREM3 |
| Bosch | Bosch; Durastar | CL3000i-Set 26 E A/C; RG10A(G2S)BGEF remote; RG10R(M2S)/BGEFU1 remote |  |
| Carrier | Carrier; Carrier/Surrey | 3021203 RR03-S-Remote (CARRIER_AC84); 342WM100CT A/C (CARRIER_AC84); 40GKX0E2006 remote (CARRIER_AC128); 42QG5A55970 remote; 53NGK009/012 Inverter; 619EGX0090E0 A/C; 619EGX0120E0 A/C; 619EGX0180E0 A/C; 619EGX0220E0 A/C |  |
| Coolix | Beko; Midea | BINR 070/071 split-type A/C; MS12FU-10HRDN1-QRD0GW(B) A/C; MSABAU-07HRFN1-QRD0GW A/C (circa 2016); RG52D/BGE Remote; RG57K7(B)/BGEF Remote |  |
| Corona | Corona | AR-01 remote; CSH-N2211 A/C; CSH-N2511 A/C; CSH-N2811 A/C; CSH-N4011 A/C |  |
| Daikin | Daikin | 17 Series FTXB09AXVJU A/C (DAIKIN128); 17 Series FTXB12AXVJU A/C (DAIKIN128); 17 Series FTXB24AXVJU A/C (DAIKIN128); ARC423A5 remote (DAIKIN160); ARC433 remote (DAIKIN); ARC433B69 remote (DAIKIN216); ARC443A5 remote (DAIKIN); ARC466A12 remote (DAIKIN); ARC466A33 remote (DAIKIN); ARC466A67 remote (DAIKIN312); ARC477A1 remote (DAIKIN2); ARC480A5 remote (DAIKIN152); ARC484A4 remote (DAIKIN216); BRC4C151 remote (DAIKIN176); BRC4C153 remote (DAIKIN176); BRC4M150W16 remote (DAIKIN200); BRC52B63 remote (DAIKIN128); DGS01 remote (DAIKIN64); FFN-C/FCN-F Series A/C (DAIKIN64); FFQ35B8V1B A/C (DAIKIN176); FTE12HV2S A/C; FTQ60TV16U2 A/C (DAIKIN216); FTWX35AXV1 A/C (DAIKIN64); FTXM-M A/C (DAIKIN); FTXM20R5V1B A/C (DAIKIN312); FTXZ25NV1B A/C (DAIKIN2); FTXZ35NV1B A/C (DAIKIN2); FTXZ50NV1B A/C (DAIKIN2); M Series A/C (DAIKIN) |  |
| Delonghi | Delonghi | PAC A95 |  |
| Ecoclim | EcoClim | HYSFR-P348 remote; ZC200DPO A/C |  |
| Electra | AEG; AUX; Centek; Electra; Electrolux; Frigidaire; Subtropic | Chillflex Pro AXP26U338CW A/C; Classic INV 17 / AXW12DCS A/C; FGPC102AB1 A/C; KFR-35GW/BpNFW=3 A/C; SCT-65Q09 A/C; SUB-07HN1_18Y A/C; YKR-H/102E remote; YKR-H/531E A/C; YKR-M/003E remote; YKR-P/002E remote; YKR-T/011 remote |  |
| Eurom | Eurom | Polar 16CH |  |
| Fujitsu | Fujitsu; Fujitsu General; OGeneral | AGTV14LAC A/C (ARRAH2E); AOHG09LLC A/C (ARRAH2E); AR-DB1 remote (ARDB1); AR-DL10 remote (ARDB1); AR-JW17 remote (ARDB1); AR-JW19 remote (ARDB1); AR-JW2 remote (ARJW2); AR-RAC1E remote (ARRAH2E); AR-RAE1E remote (ARRAH2E); AR-RAH1U remote (ARREB1E); AR-RAH2E remote (ARRAH2E); AR-RAH2U remote (ARRAH2E); AR-RCE1E remote (ARRAH2E); AR-RCL1E remote (ARRAH2E); AR-REB1E remote (ARREB1E); AR-REB4E remote (ARREB1E); AR-REG1U remote (ARRAH2E); AR-REW1E remote (ARREW4E); AR-REW4E remote (ARREW4E); AR-RY4 remote (ARRY4); ASHG09LLCA A/C (ARRAH2E); AST9RSGCW A/C (ARDB1); ASTB09LBC A/C (ARRY4); ASTG09K A/C (ARREW4E); ASTG18K A/C (ARREW4E); ASU12RLF A/C (ARREB1E); ASU30C1 A/C (ARDB1); ASYG09KETA-B A/C (ARREW4E); ASYG30LFCA A/C (ARRAH2E); ASYG7LMCA A/C (ARREB1E) | ARDB1; ARJW2; ARRAH2E; ARREB1E; ARREW4E; ARRY4 |
| Goodweather | Goodweather | ZH/JT-03 remote |  |
| Gree | Amana; Cooper & Hunter; EKOKAI; Gree; Green; RusClimate; Soleus Air; Ultimate; Vailland | A/C; CH-S09FTXG A/C; EACS/I-09HAR_X/N3 A/C; Heat Pump; PBC093G00CC A/C; VAI5-035WNI A/C; VIR09HP115V1AH A/C; VIR12HP230V1AH A/C; YAA1FBF remote; YACIFB remote; YAN1F1 remote; YAW1F remote; YB1F2 remote; YB1F2F remote; YBOFB remote; YBOFB2 remote; YX1F2F remote (YX1FSF); YX1FF remote; window A/C (YX1FSF) | YAW1F; YBOFB; YX1FSF |
| Haier | Daichi; Haier; Mabe | D-H A/C (HAIER_AC176); HSU-09HMC203 A/C (HAIER_AC_YRW02); HSU07-HEA03 remote (HAIER_AC); KFR-26GW/83@UI-Ge A/C (HAIER_AC160); MMI18HDBWCA6MI8 A/C (HAIER_AC176); V12843 HJ200223 remote (HAIER_AC176); V9014557 M47 8D remote (HAIER_AC176); YR-W02 remote (HAIER_AC_YRW02) |  |
| Hitachi | Hitachi | KAZE-312KSDP A/C (HITACHI_AC1); LT0541-HTA remote  (HITACHI_AC1); PC-LH3B (HITACHI_AC3); R-LT0541-HTA/Y.K.1.1-1 V2.3 remote (HITACHI_AC1); RAK-25NH5 A/C (HITACHI_AC264); RAR-2P2 remote (HITACHI_AC264); RAR-3U3 remote (HITACHI_AC296); RAR-8P2 remote (HITACHI_AC424); RAS-22NK A/C (HITACHI_AC344); RAS-35THA6 remote; RAS-70YHA3 A/C (HITACHI_AC296); RAS-AJ25H A/C (HITACHI_AC424); RF11T1 remote (HITACHI_AC344); Series VI A/C (Circa 2007) (HITACHI_AC1) |  |
| Kelon | Hisense; Kelon | AST-09UW4RVETG00A A/C (KELON168); DG11R2-01 remote (KELON168); ON/OFF 9000-12000 (KELON); RCH-R0Y3 remote (KELON168) |  |
| Kelvinator | Gree; Kelvinator; Sharp | A5VEY A/C; KSV26CRC A/C; KSV26HRC A/C; KSV35CRC A/C; KSV35HRC A/C; KSV53HRC A/C; KSV62HRC A/C; KSV70CRC A/C; KSV70HRC A/C; KSV80HRC A/C; YALIF Remote; YAP0F8 remote; YAPOF3 remote; YB1FA remote |  |
| LG | General Electric; LG | 6711A20083V remote (LG - LG6711A20083V); 6711AR2853M Remote (LG - GE6711AR2853M); A4UW30GFA2 A/C (LG2 - AKB74955603 & AKB73757604); AG1BH09AW101 A/C (LG - GE6711AR2853M); AKB73315611 remote (LG2 - AKB74955603); AKB73757604 remote (LG2 - AKB73757604); AKB74395308 remote (LG2); AKB74955603 remote (LG2 - AKB74955603); AKB75215403 remote (LG2); AMNW09GSJA0 A/C (LG2 - AKB74955603); AMNW24GTPA1 A/C (LG2 - AKB73757604); MS05SQ NW0 A/C (LG2 - AKB74955603); S4-W12JA3AA A/C (LG2); TS-H122ERM1 remote (LG - LG6711A20083V) |  |
| Midea | Comfee; Danby; Kaysun; Keystone; Lennox; Midea; MrCool; Pioneer System; Trotec | Casual CF A/C (MIDEA); DAC080BGUWDB (MIDEA); DAC100BGUWDB (MIDEA); DAC120BGUWDB (MIDEA); FS40-7AR Stand Fan (MIDEA24); M22A indoor split A/C (MIDEA); M33A indoor split A/C (MIDEA); M33B indoor split A/C (MIDEA); MCFA indoor split A/C (MIDEA); MCFB indoor split A/C (MIDEA); MMDA indoor split A/C (MIDEA); MMDB indoor split A/C (MIDEA); MPD1-12CRN7 A/C (MIDEA); MWMA indoor split A/C (MIDEA); MWMA009S4-3P A/C (MIDEA); MWMA012S4-3P A/C (MIDEA); MWMB indoor split A/C (MIDEA); R09C/BCGE remote (MIDEA); RG57A6/BGEFU1 remote (MIDEA); RG57H(B)/BGE remote (MIDEA); RG57H3(B)/BGCEF-M remote (MIDEA); RG57H4(B)BGEF remote (MIDEA); RG66B6(B)/BGEFU1 remote (MIDEA); RUBO18GMFILCAD A/C (18K BTU) (MIDEA); RYBO12GMFILCAD A/C (12K BTU) (MIDEA); TROTEC PAC 2100 X (MIDEA); TROTEC PAC 3900 X (MIDEA); UB018GMFILCFHD A/C (12K BTU) (MIDEA); WS012GMFI22HLD A/C (12K BTU) (MIDEA); WS018GMFI22HLD A/C (12K BTU) (MIDEA) |  |
| Mirage | Maxell; Mirage; Tronitechnik | KKG29A-C1 remote; KKG9A-C1 remote; MX-CH18CF A/C; Reykir 9000 A/C; VLU series A/C | KKG29AC1; KKG9AC1 |
| Mitsubishi | Mitsubishi; Mitsubishi Electric | 001CP T7WE10714 remote (MITSUBISHI136); HC3000 Projector (MITSUBISHI2); KM14A 0179213 remote; KPOA remote (MITSUBISHI112); MLZ-RX5017AS A/C (MITSUBISHI_AC); MS-GK24VA A/C; MSH-A24WV A/C (MITSUBISHI112); MSZ-FHnnVE A/C (MITSUBISHI_AC); MSZ-GV2519 A/C (MITSUBISHI_AC); MSZ-SF25VE3 A/C (MITSUBISHI_AC); MSZ-ZW4017S A/C (MITSUBISHI_AC); MUH-A24WV A/C (MITSUBISHI112); PAR-FA32MA remote (MITSUBISHI136); PEAD-RP71JAA Ducted A/C (MITSUBISHI136); RH151 remote (MITSUBISHI_AC); RH151/M21ED6426 remote (MITSUBISHI_AC); SG153/M21EDF426 remote (MITSUBISHI_AC); SG15D remote (MITSUBISHI_AC); TV (MITSUBISHI) |  |
| MitsubishiHeavy | Mitsubishi Heavy Industries | RKX502A001C remote (88 bit); RLA502A700B remote (152 bit); SRKxxZJ-S A/C (88 bit); SRKxxZM-S A/C (152 bit); SRKxxZMXA-S A/C (152 bit) |  |
| Neoclima | Neoclima; Soleus Air | NS-09AHTI A/C; TTWM1-10-01 A/C; ZCF/TL-05 remote; ZH/TY-01 remote |  |
| Panasonic | Panasonic | A75C2295 remote (PANASONIC_AC32); A75C2311 remote (PANASONIC_AC CKP/5); A75C2616-1 remote (PANASONIC_AC DKE/3); A75C3704 remote (PANASONIC_AC DKE/3); A75C3747 remote (PANASONIC_AC JKE/4); A75C4762 remote (PANASONIC_AC RKR/6); CKP series A/C (PANASONIC_AC CKP/5); CS-E12QKEW A/C (PANASONIC_AC DKE/3); CS-E7PKR A/C (PANASONIC_AC DKE/2); CS-E9CKP series A/C (PANASONIC_AC32); CS-ME10CKPG A/C (PANASONIC_AC CKP/5); CS-ME12CKPG A/C (PANASONIC_AC CKP/5); CS-ME14CKPG A/C (PANASONIC_AC CKP/5); CS-YW9MKD A/C (PANASONIC_AC JKE/4); CS-Z24RKR A/C (PANASONIC_AC RKR/6); CS-Z9RKR A/C (PANASONIC_AC RKR/6); DKE series A/C (PANASONIC_AC DKE/3); DKW series A/C (PANASONIC_AC DKE/3); JKE series A/C (PANASONIC_AC JKE/4); NKE series A/C (PANASONIC_AC NKE/2); PKR series A/C (PANASONIC_AC DKE/3); PN1122V remote (PANASONIC_AC DKE/3); RKR series A/C (PANASONIC_AC RKR/6); TV (PANASONIC) | CKP; DKE; JKE; LKE; NKE; RKR |
| Rhoss | Rhoss | Idrowall MPCV 20-30-35-40 |  |
| Samsung | Samsung | AH59-02692E Soundbar remote (SAMSUNG36); AK59-00167A Bluray remote (SAMSUNG36); AR09FSSDAWKNFA A/C (SAMSUNG_AC); AR09HSFSBWKN A/C (SAMSUNG_AC); AR12HSSDBWKNEU A/C (SAMSUNG_AC); AR12KSFPEWQNET A/C (SAMSUNG_AC); AR12NXCXAWKXEU A/C (SAMSUNG_AC); AR12TXEAAWKNEU A/C (SAMSUNG_AC); BN59-01178B TV remote (SAMSUNG); DB63-03556X003 remote; DB93-14195A remote (SAMSUNG_AC); DB93-16761C remote; DB96-24901C remote (SAMSUNG_AC); HW-J551 Soundbar (SAMSUNG36); IEC-R03 remote; UA55H6300 TV (SAMSUNG); UE40K5510AUXRU TV (SAMSUNG) |  |
| Sanyo | Sanyo | LC7461 transmitter IC (SANYO_LC7461); RCS-2HS4E remote (SANYO_AC); RCS-2S4E remote (SANYO_AC); RCS-4MHVPIS4EE remote (SANYO_AC152); SA 8650B - disabled; SAP-K121AHA A/C (SANYO_AC); SAP-K242AH A/C (SANYO_AC); SAP-KMRV124EHE A/C (SANYO_AC152) |  |
| Sharp | Sharp | AH-A12REVP-1 A/C (A903); AH-AxSAY A/C (A907); AH-PR13-GL A/C (A903); AH-XP10NRY A/C (A903); AY-ZP40KR A/C (A907); CRMC-820 JBEZ remote (A903); CRMC-A705 JBEZ remote (A705); CRMC-A863 JBEZ remote (A903); CRMC-A903JBEZ remote (A903); CRMC-A907 JBEZ remote (A907); CRMC-A950 JBEZ (A907); LC-52D62U TV | A705; A903; A907 |
| Tcl | Daewoo; Electrolux; Leberg; TCL; Teknopoint | Allegro SSA-09H A/C (GZ055BE1); DSB-F0934ELH-V A/C; EACM CL/N3 series remote; GYKQ-52E remote; GYKQ-58(XM) remote (TCL96AC); GZ-055B-E1 remote (GZ055BE1); LBS-TOR07 A/C (TAC09CHSD); TAC-09CHSD/XA31I A/C (TAC09CHSD) | GZ055BE1; TAC09CHSD |
| Technibel | Technibel | IRO PLUS |  |
| Teco | Alaska | SAC9010QC A/C; SAC9010QC remote |  |
| Toshiba | Carrier; Toshiba | 42NQV025M2 / 38NYV025M2 A/C; 42NQV035M2 / 38NYV035M2 A/C; 42NQV050M2 / 38NYV050M2 A/C; 42NQV060M2 / 38NYV060M2 A/C; Akita EVO II; RAS 18SKP-ES; RAS-2558V A/C; RAS-25SKVP2-ND A/C; RAS-B13N3KV2; RAS-B13N3KVP-E; WC-L03SE; WH-TA01JE remote; WH-TA04NE; WH-UB03NJ remote | GENERICREMOTE_A; GENERICREMOTE_B |
| Transcold | Transcold | M1-F-NO-6 A/C |  |
| Trotec | Duux; Trotec | Blizzard Smart 10K / DXMA04 A/C (TROTEC); PAC 3200 A/C (TROTEC); PAC 3550 Pro A/C (TROTEC_3550) |  |
| Truma | Truma | 40091-86700 remote; Aventa A/C |  |
| Vestel | Vestel | BIOX CXP-9 A/C (9K BTU) |  |
| Voltas | Voltas | 122LZF 4011252 Window A/C | 122LZF |
| Whirlpool | Whirlpool | DG11J1-04 remote; DG11J1-3A remote; DG11J1-91 remote; SPIS409L A/C; SPIS412L A/C; SPIW409L A/C; SPIW412L A/C; SPIW418L A/C | DG11J13A; DG11J191 |
| York | York | GRYLH2A remote; MHH07P17 A/C |  |
