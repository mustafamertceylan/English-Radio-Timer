# Radio_Timer — Proje Yöneticisi Karar Dokümanı (Tur 1)

Tarih: 2026-11-25 · Hazırlayan: Ajan 1 (Proje Yöneticisi)
Girdi: Ajan 4 kullanışlılık raporu (U1–U12), Ajan 5 denetim raporu (H1–H25), kaynak kodun incelenmesi.

**Tur hedefi:** Tüm kritik ve yüksek hatalar, orta seviye hatalar, düşük eforlu düşük hatalar ve küçük/orta eforlu yüksek değerli kullanışlılık önerileri. Tur sonunda `.\gradlew.bat assembleDebug` başarılı olmalı.
**Kapsam dışı (backlog):** Room şema değişikliği / migration, MediaSessionService geçişi, minSdk değişikliği, grafik kütüphanesi, L eforlu işler.

---

## 0. Özet tablo

| Karar | Adet | Maddeler |
|---|---|---|
| KABUL | 9 U + 25 H = **34** | U1, U2, U3, U4, U5, U6, U7, U12 (kısmi), U11 (yalnız H15 kapsamı, kısmi) · H1–H25 (H17 ve H21 kısmi) |
| ERTELE | **4** | U8, U9, U10, U11'in kalan kısmı (ayrıca H17'nin migration kısmı) |
| RET | **0** | — |

Faz planı:
- **Faz A** (bitti): bu doküman.
- **Faz B**: Ajan 2 (UI) ve Ajan 3 (Backend) paralel uygulama. Dosya sahipliği aşağıda; sahibi olmadığın dosyaya dokunma.
- **Faz C**: Entegrasyon derlemesi (`.\gradlew.bat assembleDebug`). Hata hangi dosyadaysa o dosyanın sahibi düzeltir. Not: Backend `R.string.*` anahtarlarını UI eklediği için, UI de Backend sınıflarını kullandığı için **iki taraf tamamlanmadan tek başına derleme başarısız olabilir — bu beklenen bir durum.** Her ajan kendi tarafında sözleşmeye harfiyen uymalı.
- **Faz D**: Ajan 4 kabul edilen U maddelerini, Ajan 5 kabul edilen H maddelerini doğrular (kontrol listesi §6). Ajan 1 CLAUDE.md "Değişiklik Günlüğü" ve "Bilinen sorunlar" bölümlerini günceller.

---

## 1. Kararlar

### 1.1 Kullanışlılık önerileri (Ajan 4)

| # | Karar | Atanan | Gerekçe |
|---|---|---|---|
| U1 Oynatma durumu geri bildirimi | **KABUL** | Backend (durum + broadcast) / UI (durum metni) | H5 ile aynı kök sorun; "çalıyor görünen sessizlik" en büyük UX açığı. |
| U2 Aksan çeşitli istasyon listesi + HLS | **KABUL** | Backend (StationRepository, HLS) / UI (aksan etiketi) | H6'yı çözer; doğrulanmış 6 URL var. ABC/RNZ eklenmeyecek. |
| U3 Hedef kısayol chip'leri | **KABUL** | UI | S efor, tema zaten Material3 (Chip destekli). |
| U4 Hedef kutlama bildirimi + %100 sınırı | **KABUL** | Backend (bildirim) / UI (%100 sınırı, "Hedefe ulaşıldı" metni) | S efor, motivasyon değeri yüksek; H12 ile birleşik. |
| U5 Seri (streak) | **KABUL** | Backend (DAO + StatsHelper) / UI (tarih kartında gösterim) | Şema değişikliği gerektirmiyor (`isGoalReached` mevcut). |
| U6 Son istasyonu hatırla | **KABUL** | Backend (PreferenceHelper + servis) / UI (açılışta isim) | S efor; H10 ile doğal uyum. |
| U7 Takvim gün dokunma + ay değişimi | **KABUL** | UI (DAO/DateKeys Backend'den) | H13 ile aynı dosya, aynı iş. |
| U8 Haftalık istatistik/grafik | **ERTELE** | — | M efor + yeni ekran/kütüphane; bu tur kapsamı zaten büyük. |
| U9 Uyku zamanlayıcı | **ERTELE** | — | Orta değer; servis bu tur ciddi yeniden yazılıyor, üstüne yeni durum makinesi eklemek riskli. Sonraki turda `ACTION_SLEEP_TIMER` olarak eklenecek (sabit adı şimdiden rezerve, uygulanmayacak). |
| U10 Kullanıcı istasyon ekleme | **ERTELE** | — | L efor, Room v2 + migration. Kapsam dışı. |
| U11 Karanlık tema uyumu | **KISMİ KABUL** | UI | Yalnızca H15 kapsamı (renk token'ları + values-night). Tam tema/ikon/takvim teması cilası backlog. |
| U12 Erişilebilirlik + strings.xml | **KISMİ KABUL** | UI | Dokunulan tüm layout ve Activity metinleri strings.xml'e; ikonlara `contentDescription`. Kapsamlı TalkBack testi backlog. |

### 1.2 Denetim bulguları (Ajan 5)

| # | Seviye | Karar | Atanan | Not |
|---|---|---|---|---|
| H1 | Kritik | **KABUL** | Backend | PLAY_STATION/TOGGLE'da koşulsuz `startForeground` ("Bağlanıyor…"). |
| H2 | Kritik | **KABUL** | Backend (kanal, broadcast setPackage) + UI (`ContextCompat.registerReceiver`) | |
| H3 | Yüksek | **KABUL** | Backend | `currentDayKey`; tick'te tarih değişirse eskiyi kaydet, sıfırla. |
| H4 | Yüksek | **KABUL** | Backend | `isLoaded` + max birleştirme, tek `AppExecutors.diskIO()`, değer parametreyle. |
| H5 | Yüksek | **KABUL** | Backend | `onPlayerError`, `isPlayerReady` reset, durum makinesi (§2.2). |
| H6 | Yüksek | **KABUL** | Backend (liste) + UI (MainActivity'den listeyi kaldır) | Tek kaynak: `StationRepository`. |
| H7 | Orta | **KABUL** | Backend + UI | Paket önekli action, not-exported alıcı. |
| H8 | Orta | **KABUL** | Backend | `ACTION_STOP`, bildirimde "Kapat", `START_NOT_STICKY`, onCreate'te prepare yok. |
| H9 | Orta | **KABUL** | Backend | Audio focus, becoming noisy, wake mode + `WAKE_LOCK`. |
| H10 | Orta | **KABUL** | Backend (`ACTION_REQUEST_STATE`) + UI (onResume'da iste) | |
| H11 | Orta | **KABUL** | UI | "Hedef" etiketine id, `currentDateText` set, takvim başlığı dinamik. |
| H12 | Orta | **KABUL** | UI (StatsHelper Backend'den) | `StatsHelper.progressPercent` 0–100 sınırlı. |
| H13 | Orta | **KABUL** | UI | Kayıtlı `isGoalReached` kullan; bugün kırmızı değil; ay değişince başlık+toplam. |
| H14 | Orta | **KABUL** | UI | SettingsActivity insets. |
| H15 | Orta | **KABUL** | UI | Sabit `#FFFFFF` vb. → `@color/` token; `values-night/colors.xml`. |
| H16 | Orta | **KABUL** | Backend | `usesCleartextTraffic` kaldırılır (tüm URL'ler https). |
| H17 | Düşük | **KISMİ KABUL** | Backend (+ UI ham Thread'ler) | COALESCE ve `AppExecutors` bu tur. `fallbackToDestructiveMigration` kaldırma → **ERTELE** (bu tur şema değişmiyor, v1 kalıyor; U10 ile birlikte gerçek migration yazılacak). |
| H18 | Düşük | **KABUL** | UI (giriş) + Backend (PreferenceHelper clamp) | Virgül kabul; min 1 dk. |
| H19 | Düşük | **KABUL** | Backend (TimeFormatter) + UI (kullanım) | "sa dk sn". |
| H20 | Düşük | **KABUL** | Backend | Cache'li builder, yalnız metin/durum değişince notify, contentIntent, hedef aşılınca farklı metin. |
| H21 | Düşük | **KISMİ KABUL** | UI (ölü kod, `bbcCard`→`nowPlayingCard`) + Backend (CountDownTimer → Handler) | Hepsi bu tur; "kısmi" sadece kapsam genişletilmeyecek anlamında. |
| H22 | Düşük | **KABUL** | Backend (DateKeys) + UI (kullanım) | `Locale.US`. |
| H23 | Düşük | **KABUL** | Backend | STOP/TOGGLE/yeni istasyon bekleyen çalmayı iptal eder. |
| H24 | Düşük | **KABUL** | Backend | Tek dependencies bloğu, media3-ui çıkar, HLS + cardview ekle. |
| H25 | Şüpheli | **KABUL (önleyici)** | Backend | material-calendarview 2.0.1 POM'unda `threetenabp:1.1.1` *runtime* scope'ta — init çağrısı yok. Düşük riskli önlem: bağımlılığı açıkça ekle + `RadioTimerApp` Application'da `AndroidThreeTen.init(this)`. Faz D'de takvim açılışı cihazda doğrulanır. |

---

## 2. ARAYÜZ SÖZLEŞMESİ (bağlayıcı — sapma yok)

Paket: `com.example.radio_timer`. Aşağıdaki tüm sınıf/metotları **Backend (Ajan 3)** yazar; **UI (Ajan 2)** bunların var olduğunu varsayarak kullanır. İmza değişikliği gerekirse Ajan 1'e bildirilir, tek taraflı değiştirilmez.

### 2.1 Servis → Activity broadcast

```java
// RadioService.java içinde
public static final String ACTION_STATE_UPDATE = "com.example.radio_timer.STATE_UPDATE";
```
- Eski `TIMER_UPDATE` sabiti **silinir**.
- Gönderim: `Intent i = new Intent(ACTION_STATE_UPDATE); i.setPackage(getPackageName()); sendBroadcast(i);`
- Ne zaman: (a) çalma oturumu aktifken saniyede bir, (b) her durum değişiminde, (c) `ACTION_REQUEST_STATE` alındığında, (d) gün değişiminde.
- UI kaydı: `ContextCompat.registerReceiver(this, receiver, new IntentFilter(RadioService.ACTION_STATE_UPDATE), ContextCompat.RECEIVER_NOT_EXPORTED);` (onResume) / `unregisterReceiver` (onPause).

| Sabit adı | Anahtar | Tip | Değer / anlam |
|---|---|---|---|
| `EXTRA_MILLIS_PLAYED` | `"millis_played"` | `long` | Bugünkü toplam dinleme (ms). |
| `EXTRA_DAILY_GOAL` | `"daily_goal"` | `long` | Güncel hedef (ms), her zaman ≥ `PreferenceHelper.MIN_GOAL_MILLIS`. UI her saniye prefs okumaz, bunu kullanır. |
| `EXTRA_GOAL_REACHED` | `"goal_reached"` | `boolean` | `millis_played >= daily_goal`. |
| `EXTRA_DATE_KEY` | `"date_key"` | `String` | `"yyyy-MM-dd"` (DateKeys formatı). UI değişince tarih kartını/seriyi yeniler. |
| `EXTRA_IS_PLAYING` | `"is_playing"` | `boolean` | Kullanıcı çalma niyetinde mi (playWhenReady). `true` ⇒ UI duraklat ikonu gösterir. `CONNECTING`/`BUFFERING`/`PLAYING` durumlarında `true`; `PAUSED`/`IDLE`/`ERROR`'da `false`. |
| `EXTRA_PLAYER_STATE` | `"player_state"` | `String` | Aşağıdaki `STATE_*` değerlerinden biri, asla null. |
| `EXTRA_ERROR_CODE` | `"error_code"` | `String` | Yalnız `STATE_ERROR`'da dolu: `ERROR_NETWORK` / `ERROR_SOURCE` / `ERROR_UNKNOWN`. Diğer durumlarda **null**. |
| `EXTRA_ERROR_MESSAGE` | `"error_message"` | `String` | Teknik detay (`PlaybackException.getErrorCodeName()`), log/debug için; null olabilir. UI kullanıcıya göstermez. |
| `EXTRA_STATION_URL` | `"station_url"` | `String` | Aktif/son istasyon URL'si. Null olmaz (son istasyon yoksa `StationRepository.getDefault()`). |
| `EXTRA_STATION_NAME` | `"station_name"` | `String` | Aktif/son istasyon adı. Null olmaz. |

`player_state` değerleri (RadioService içinde `public static final String`):

| Sabit | Değer | Anlam | Sayaç ilerler mi |
|---|---|---|---|
| `STATE_IDLE` | `"idle"` | Hiç çalma başlatılmadı / STOP sonrası | Hayır |
| `STATE_CONNECTING` | `"connecting"` | prepare edildi, ilk STATE_READY bekleniyor | Hayır |
| `STATE_BUFFERING` | `"buffering"` | Çalarken arabellek boşaldı (STATE_BUFFERING) | Hayır |
| `STATE_PLAYING` | `"playing"` | STATE_READY + playWhenReady | **Evet** |
| `STATE_PAUSED` | `"paused"` | Kullanıcı duraklattı / audio focus kaybı / kulaklık çıkarıldı | Hayır |
| `STATE_ERROR` | `"error"` | `onPlayerError` alındı | Hayır |

Hata kodu sabitleri: `ERROR_NETWORK = "network"` (IO_NETWORK_*, timeout, bağlantı yok), `ERROR_SOURCE = "source"` (HTTP 4xx/5xx `IO_BAD_HTTP_STATUS`, `PARSING_*`, `IO_FILE_NOT_FOUND`, desteklenmeyen format), `ERROR_UNKNOWN = "unknown"` (diğer).

### 2.2 Activity → Servis action'ları

```java
public static final String ACTION_PLAY_STATION  = "com.example.radio_timer.action.PLAY_STATION";
public static final String ACTION_TOGGLE        = "com.example.radio_timer.action.TOGGLE";
public static final String ACTION_STOP          = "com.example.radio_timer.action.STOP";
public static final String ACTION_REQUEST_STATE = "com.example.radio_timer.action.REQUEST_STATE";
// ACTION_SLEEP_TIMER: U9 ertelendi — bu tur TANIMLANMAYACAK.
```

| Action | Extra | Nasıl başlatılır (UI) | Servis davranışı |
|---|---|---|---|
| `ACTION_PLAY_STATION` | `EXTRA_STATION_URL` (String, zorunlu), `EXTRA_STATION_NAME` (String, zorunlu) | API≥26 `startForegroundService`, altı `startService` | Önce koşulsuz `startForeground` (Bağlanıyor…). Aynı URL ⇒ toggle. Farklı URL ⇒ eski çalmayı durdur, bekleyen çalmayı iptal et, `isPlayerReady=false`, yeni kaynağı hazırla, `STATE_CONNECTING`, son istasyonu prefs'e yaz. |
| `ACTION_TOGGLE` | — | API≥26 `startForegroundService`, altı `startService` | Koşulsuz `startForeground`. PLAYING/BUFFERING/CONNECTING ⇒ PAUSED (bekleyen çalma iptal). PAUSED/IDLE/ERROR ⇒ son istasyonu (yoksa default) çal; ERROR/IDLE'da yeniden prepare. |
| `ACTION_STOP` | — | Yalnız bildirim aksiyonundan (UI'dan da gönderilebilir, `startService`) | Çalmayı durdur, ilerlemeyi kaydet, `STATE_IDLE` yayınla, `stopForeground(REMOVE)`, `stopSelf()`. Servis zaten çalışmıyorsa sessizce kapanır. |
| `ACTION_REQUEST_STATE` | — | **`startService`** (asla `startForegroundService`), `try/catch(IllegalStateException)` içinde | Durumu hemen yayınla. Servis aktif bir oturumda değilse (IDLE, foreground değil) DB yüklemesi bitince yayınla ve `stopSelfResult(startId)` ile kapan; `startForeground` **çağırmaz**. |

Ek kurallar:
- `onStartCommand` dönüşü: `START_NOT_STICKY`. `intent == null` ⇒ `stopSelf()`.
- Duraklatıldığında servis foreground'da kalır (bildirim "Duraklatıldı" + Devam et + Kapat). Tamamen kapatma yalnız `ACTION_STOP` ile.
- Bildirim aksiyon PendingIntent'leri: API≥26 `PendingIntent.getForegroundService`, altı `getService`; bayrak `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`; request code'lar farklı (TOGGLE=1, STOP=2). `contentIntent` = MainActivity (`getActivity`, `FLAG_IMMUTABLE`).
- Bildirim ID'leri: oynatma `1` (kanal `"radio_playback"`), hedef kutlaması `2` (kanal `"goal_events"`). Eski `"RadioChannel"` kanalı kullanılmaz.

### 2.3 Paylaşılan yardımcı sınıflar (Backend yazar)

```java
// Station.java  (değişmez/immutable)
public final class Station {
    public static final String ACCENT_UK = "UK";
    public static final String ACCENT_US = "US";
    public static final String ACCENT_CA = "CA";
    public Station(String name, String url, String accent);
    public String getName();
    public String getUrl();
    public String getAccent();   // ACCENT_* değerlerinden biri; UI string kaynağına eşler
}

// StationRepository.java
public final class StationRepository {
    public static List<Station> getStations();        // değiştirilemez liste, sıra aşağıda
    public static Station getDefault();               // BBC World Service
    @Nullable public static Station findByUrl(String url);
}

// TimeFormatter.java  (birim kısaltmaları Türkçe ve sınıf içinde sabit: "sa", "dk", "sn")
public final class TimeFormatter {
    public static String formatShort(long millis);   // "2 sa 5 dk 9 sn" | "5 dk 9 sn" | "9 sn" (baştaki sıfır birimler atlanır; 0 ⇒ "0 sn")
    public static String formatGoal(long millis);    // saniyesiz: "4 sa" | "2 sa 30 dk" | "45 dk" | <1 dk ⇒ "0 dk"
    public static String formatClock(long millis);   // "02:05:09" (Locale.US)
}

// DateKeys.java  (tüm anahtarlar Locale.US, cihaz saat diliminde)
public final class DateKeys {
    public static String today();                            // "yyyy-MM-dd"
    public static String currentMonth();                     // "yyyy-MM"
    public static String of(int year, int month1to12, int day);    // "yyyy-MM-dd"
    public static String monthKey(int year, int month1to12);       // "yyyy-MM"
    public static String previousDay(String dateKey);        // "2026-03-01" ⇒ "2026-02-28"
    public static int[] parse(String dateKey);               // {year, month1to12, day}
}

// StatsHelper.java
public final class StatsHelper {
    public static int progressPercent(long playedMillis, long goalMillis); // 0..100 sınırlı; goal<=0 ⇒ 0
    public static int computeStreak(List<String> reachedDatesDesc, String todayKey);
        // Ardışık hedef günleri. Bugün ulaşıldıysa bugünden geriye, ulaşılmadıysa dünden geriye sayar. Saf fonksiyon.
    @WorkerThread public static int loadStreak(Context context); // DAO + computeStreak; ANA THREAD'DE ÇAĞRILMAZ
}

// AppExecutors.java
public final class AppExecutors {
    public static ExecutorService diskIO(); // tek paylaşımlı SingleThreadExecutor; tüm Room okuma/yazmaları buradan
}

// RadioTimerApp.java  (extends Application) — onCreate: AndroidThreeTen.init(this). UI bu sınıfı kullanmaz.
```

`PreferenceHelper` (mevcut metotlar korunur + yeniler):
```java
public static final long MIN_GOAL_MILLIS = 60_000L;        // 1 dk
public static final long MAX_GOAL_MILLIS = 86_400_000L;    // 24 sa
public void  setDailyGoal(long millis);        // MIN..MAX aralığına sıkıştırır
public long  getDailyGoal();                   // her zaman MIN..MAX (sıfıra bölme imkânsız)
public float getDailyGoalInHours();
public void  setDailyGoalInHours(float hours); // setDailyGoal üzerinden clamp
@Nullable public String getLastStationUrl();
@Nullable public String getLastStationName();
public void  setLastStation(String url, String name);
@Nullable public String getLastCelebratedDate();   // U4: aynı gün ikinci kutlamayı engeller
public void  setLastCelebratedDate(String dateKey);
```

`ListeningDao` (mevcutlar korunur, şema/versiyon DEĞİŞMEZ — v1):
```java
@Insert(onConflict = REPLACE) void insertOrUpdate(ListeningLog log);
ListeningLog getLogByDate(String targetDate);
@Query("SELECT COALESCE(SUM(durationMillis), 0) FROM listening_logs WHERE date LIKE :monthPrefix || '%'")
long getTotalTimeForMonth(String monthPrefix);                 // "yyyy-MM"
List<ListeningLog> getAllLogs();
@Query("SELECT * FROM listening_logs WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
List<ListeningLog> getLogsForMonth(String monthPrefix);        // YENİ (U7/H13)
@Query("SELECT date FROM listening_logs WHERE isGoalReached = 1 ORDER BY date DESC")
List<String> getReachedDates();                                // YENİ (U5)
```
`ListeningLog` alanları değişmez: `date` (PK), `durationMillis`, `isGoalReached`.

İstasyon listesi (sıra sabit, hepsi https):

| # | Ad | URL | Aksan |
|---|---|---|---|
| 1 | BBC World Service | https://stream.live.vc.bbcmedia.co.uk/bbc_world_service | UK |
| 2 | LBC News | https://media-ssl.musicradio.com/LBCNewsUK | UK |
| 3 | NPR News | https://npr-ice.streamguys1.com/live.mp3 | US |
| 4 | WNYC 93.9 FM | https://fm939.wnyc.org/wnycfm | US |
| 5 | KEXP 90.3 | https://kexp-mp3-128.streamguys1.com/kexp128.mp3 | US |
| 6 | CBC Radio One | https://cbcradiolive.akamaized.net/hls/live/2041057/ES_R1ETR/master.m3u8 | CA |

### 2.4 String kaynakları — sahiplik kararı

**Karar:** `res/` klasörünün tamamı yalnız UI'a (Ajan 2) aittir. Backend hiçbir res dosyası oluşturmaz/düzenlemez; ihtiyaç duyduğu metinleri `getString(R.string.<anahtar>)` ile kullanır ve UI bu anahtarları **birebir aşağıdaki isim ve metinle** `values/strings.xml`'e ekler. İstisna: `TimeFormatter` birim kısaltmaları ("sa", "dk", "sn") Java içinde sabittir.

**Backend'in ihtiyaç duyduğu, UI'ın eklemesi ZORUNLU anahtarlar:**

| Anahtar | Metin |
|---|---|
| `notif_channel_playback` | Radyo oynatma |
| `notif_channel_goal` | Hedef bildirimleri |
| `notif_connecting` | Bağlanıyor… |
| `notif_buffering` | Arabelleğe alınıyor… |
| `notif_paused` | Duraklatıldı |
| `notif_error` | Yayın açılamadı. Tekrar denemek için Devam et'e dokunun. *(XML'de kesme işareti `\'` olarak kaçırılmalı)* |
| `notif_remaining` | Kalan süre: %1$s |
| `notif_goal_done_ongoing` | Günlük hedef tamamlandı · Bugün %1$s |
| `notif_action_pause` | Duraklat |
| `notif_action_play` | Devam et |
| `notif_action_stop` | Kapat |
| `notif_goal_reached_title` | Tebrikler! |
| `notif_goal_reached_text` | Bugünkü %1$s dinleme hedefine ulaştın. |
| `calendar_title` | Çalışma Takvimi *(Manifest'teki CalendarActivity label'ı için)* |

**UI'ın kendi kullanımı için tanımlayacağı anahtarlar (minimum; UI ekleyebilir, adları UI belirler ama şu adlar önerilir):**
`app_title`, `goal_label` ("Hedef: %1$s"), `percent_done` ("%%%1$d tamamlandı"), `goal_reached_label` ("Hedefe ulaşıldı!"), `stations_header`, `view_calendar`, `streak_label` ("Seri: %1$d gün"), `streak_none` ("Seriyi bugün başlat"), `state_idle` ("Başlamak için bir istasyon seç"), `state_connecting` ("Bağlanıyor…"), `state_buffering` ("Arabelleğe alınıyor…"), `state_playing` ("Çalıyor"), `state_paused` ("Duraklatıldı"), `error_network` ("Bağlantı hatası. İnternetinizi kontrol edin."), `error_source` ("Bu istasyon şu an yayın vermiyor."), `error_unknown` ("Çalma hatası oluştu."), `accent_uk` ("İngiliz aksanı"), `accent_us` ("Amerikan aksanı"), `accent_ca` ("Kanada aksanı"), `settings_*` (başlık, ipucu, kaydet, hata mesajları), `chip_15m`/`chip_30m`/`chip_1h`/`chip_2h`/`chip_4h`, `calendar_month_total` ("Aylık toplam: %1$s"), `calendar_day_detail` ("%1$s: %2$s"), `calendar_day_empty` ("%1$s: kayıt yok"), `cd_settings`, `cd_play_pause`, `cd_back` (contentDescription), izin diyaloğu metinleri, mevcut `English_Listening` korunur.

---

## 3. Dosya sahipliği

| Ajan | Sahip olduğu dosyalar |
|---|---|
| **Backend (Ajan 3)** | `RadioService.java`, `AppDatabase.java`, `ListeningDao.java`, `ListeningLog.java`, `PreferenceHelper.java`, `Station.java`, **yeni:** `StationRepository.java`, `TimeFormatter.java`, `DateKeys.java`, `StatsHelper.java`, `AppExecutors.java`, `RadioTimerApp.java`; `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml` |
| **UI (Ajan 2)** | `MainActivity.java`, `SettingsActivity.java`, `CalendarActivity.java`, `StationAdapter.java`, `res/**` (layout, values, values-night, drawable, xml) |
| **Ajan 4** | Bu turda dosya yazmaz. Faz D doğrulayıcı (U maddeleri). |
| **Ajan 5** | Faz D doğrulayıcı (H maddeleri). |
| **Ajan 1** | `CLAUDE.md` (Faz D sonunda günlük güncellemesi). |

---

## 4. Backend iş listesi (Ajan 3)

1. **[H24, U2-HLS, H25] Build:** `app/build.gradle.kts`'te tek `dependencies` bloğu. `media3-ui` kaldır. Ekle: `androidx.media3:media3-exoplayer-hls:1.2.1`, `androidx.cardview:cardview:1.0.0`, `com.jakewharton.threetenabp:threetenabp:1.1.1`. Media3/Room sürümlerini `libs.versions.toml`'a taşımak serbest (tavsiye: `media3 = "1.2.1"`, `room = "2.6.1"`). Başka sürüm yükseltmesi YAPMA (compileSdk/AGP/minSdk dokunulmaz).
2. **[H16, H9, H25, H11] Manifest:** `usesCleartextTraffic` kaldır; `android.permission.WAKE_LOCK` ekle; `<application android:name=".RadioTimerApp">`; CalendarActivity `android:label="@string/calendar_title"`. Servis tanımı (`mediaPlayback`, exported=false) korunur.
3. **[Yeni sınıflar]** §2.3'teki imzalarla birebir: `Station` (accent), `StationRepository` (§2.3 tablosu), `TimeFormatter`, `DateKeys` (Locale.US), `StatsHelper`, `AppExecutors`, `RadioTimerApp`.
4. **[H17, U5, U7] DAO:** COALESCE, `getLogsForMonth`, `getReachedDates`. `AppDatabase` versiyonu 1 kalır; `fallbackToDestructiveMigration` bu tur kalır.
5. **[H18, U6, U4] PreferenceHelper:** §2.3 yeni sabit/metotlar; clamp.
6. **[H2] Bildirim kanalları:** `createNotificationChannels()` yalnız `Build.VERSION.SDK_INT >= O` iken; iki kanal (`radio_playback` IMPORTANCE_LOW, `goal_events` IMPORTANCE_DEFAULT).
7. **[H8] Yaşam döngüsü:** `onCreate`'te `prepare` YOK (yalnız player kurulumu + `loadInitialProgress`). `START_NOT_STICKY`; `intent==null` ⇒ `stopSelf`. `ACTION_STOP` uygulanır.
8. **[H1] Foreground:** `PLAY_STATION`/`TOGGLE` dalında ilk iş olarak koşulsuz `startForeground(1, buildNotification())`. `REQUEST_STATE`'te çağrılmaz (§2.2).
9. **[H9] Player kurulumu:** `setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)`, `setHandleAudioBecomingNoisy(true)`, `setWakeMode(C.WAKE_MODE_NETWORK)`. Focus kaybı/noisy sonucu `playWhenReady=false` olursa `onPlayWhenReadyChanged` üzerinden `STATE_PAUSED` + kayıt.
10. **[U2, H6] Medya kaynağı:** `ProgressiveMediaSource` yerine `DefaultMediaSourceFactory(context).setDataSourceFactory(httpFactory)` (User-Agent + cross-protocol redirect korunur) — `.m3u8` HLS otomatik algılanır. Player builder'a `setMediaSourceFactory` ile verilebilir.
11. **[H5, U1, H23] Durum makinesi:** §2.1 `STATE_*` geçişleri; `onPlaybackStateChanged`, `onPlayWhenReadyChanged`, `onPlayerError` dinleyicileri. Hata ⇒ sayaç durur, kayıt, `STATE_ERROR` + `error_code`. İstasyon değişiminde `isPlayerReady=false`, `pendingPlay` iptal. Tavsiye: `pendingPlay` bayrağı yerine doğrudan `exoPlayer.setPlayWhenReady(true)` + durumdan türetme.
12. **[H21] Sayaç:** `CountDownTimer(Long.MAX_VALUE)` yerine ana thread `Handler` + 1 sn `Runnable`. Sayaç yalnız `STATE_PLAYING`'de artar. Her 60 sn'de bir kayıt.
13. **[H3] Gün değişimi:** `currentDayKey` alanı; tick'te `DateKeys.today()` farklıysa eski günün toplamını eski anahtarla kaydet, `timePlayedToday=0`, `currentDayKey` güncelle, broadcast.
14. **[H4] Yarış:** `loadInitialProgress` `AppExecutors.diskIO()` üzerinde; sonuç ana thread'e post edilir ve `timePlayedToday = max(timePlayedToday, dbValue)`; `isLoaded=false` iken DB'ye yazma yapılmaz (yükleme bitince ilk kayıt). `saveProgress(String dateKey, long millis, long goal)` değerleri parametre olarak alır, yalnız `diskIO()`'ya gönderir. `onDestroy`'da kayıt `release`'ten önce.
15. **[H20, H8] Bildirim:** cache'li `NotificationCompat.Builder`; içerik = istasyon adı + durum metnine göre (`notif_connecting` / `notif_buffering` / `notif_paused` / `notif_error` / `notif_remaining` (`TimeFormatter.formatGoal(kalan)`) / hedef aşıldıysa `notif_goal_done_ongoing`). Aksiyonlar: Duraklat|Devam et + Kapat. `contentIntent` MainActivity. `NotificationManager.notify` yalnız metin veya durum değiştiğinde (dakika granülerliği ⇒ ~dakikada bir).
16. **[U4] Kutlama:** sayaç hedefi geçtiği anda (önceki < goal ≤ yeni) ve `getLastCelebratedDate() != today` ise ID 2 ile `goal_events` kanalına bildirim (`notif_goal_reached_title/text`, `formatGoal(goal)`), `setLastCelebratedDate(today)`, anında DB kaydı (`isGoalReached=true`). API 33+ izin yoksa sessizce atla (`NotificationManagerCompat.areNotificationsEnabled()`).
17. **[U6] Son istasyon:** `PLAY_STATION`'da `setLastStation`; servis başlangıcında `currentStation = findByUrl(getLastStationUrl())`, yoksa `getDefault()`. Prefs'teki URL artık listede yoksa default.
18. **[H7, H10] Broadcast:** §2.1 tüm extra'lar, `setPackage`. `REQUEST_STATE` §2.2'ye göre.
19. **[H22]** Servisteki tüm tarih üretimi `DateKeys` üzerinden.
20. Bitirince: Backend dosyalarında `R.string` dışında derleme hatası olmadığından emin ol; tam derleme Faz C'de.

## 5. UI iş listesi (Ajan 2)

1. **[H6, U2] İstasyonlar:** MainActivity'deki sabit liste silinir; `StationRepository.getStations()`. `item_station.xml`'e aksan etiketi (`stationAccent` TextView veya küçük chip) — `Station.ACCENT_*` ⇒ `accent_uk/us/ca`. Servise `RadioService.EXTRA_STATION_URL/NAME` sabitleriyle gönder.
2. **[H2, H7] Alıcı:** `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)` + `RadioService.ACTION_STATE_UPDATE`; extra'lar yalnız `RadioService.EXTRA_*` sabitleriyle okunur (string literal yok).
3. **[H10, U6] Durum geri yükleme:** onResume'da alıcıyı kaydet, `ACTION_REQUEST_STATE`'i `startService` ile `try/catch(IllegalStateException)` içinde gönder. Broadcast gelmeden önce üst kartta `PreferenceHelper.getLastStationName()` (null ise `StationRepository.getDefault().getName()`) ve DB'den bugünkü ilerleme gösterilir.
4. **[U1] Durum metni:** `activity_main.xml` üst karta `playerStatusText`; `player_state` ⇒ `state_*` metni; `STATE_ERROR`'da `error_code` ⇒ `error_*` metni (hata rengi). Adapter'da çalan satır ikonu `is_playing`'e göre; CONNECTING/BUFFERING'de satırda küçük ProgressBar veya "Bağlanıyor…" metni (`stationTimer` alanı bunun için kullanılabilir).
5. **[H12, U4] Yüzde:** `StatsHelper.progressPercent`; `goal_reached=true` ise `goal_reached_label`. Süre metni `TimeFormatter.formatShort(played) + " / " + TimeFormatter.formatGoal(goal)`.
6. **[H11, H21, U12] Ana ekran:** "Hedef: 4 saat"e id `goalLabelText` ver ve `goal_label` + `formatGoal` ile doldur; `currentDateText`'i bugünün tarihiyle doldur (`DateFormat` ile, `Locale("tr")` uygun) ve `date_key` değişince yenile; `bbcCard` ⇒ `nowPlayingCard`; ölü `currentStationUrl/Name` alanları ve boş `onDestroy` silinir; tüm sabit metinler strings.xml'e; ikonlara `contentDescription`.
7. **[U5] Seri:** tarih kartına `streakText`; onResume'da ve `date_key`/`goal_reached` değişiminde `AppExecutors.diskIO().execute(() -> { int s = StatsHelper.loadStreak(ctx); runOnUiThread(...) })`. 0 ⇒ `streak_none`.
8. **[H17] Ham Thread'ler:** MainActivity/CalendarActivity'deki `new Thread` ⇒ `AppExecutors.diskIO()`.
9. **[H13, U7, H11, H22] Takvim:** `getLogsForMonth(DateKeys.monthKey(y,m))`; boyama `log.isGoalReached` ile; bugün hedef dolmadıysa kırmızı boyanmaz (nötr/boyasız). `setOnMonthChangedListener` ⇒ `calendarHeaderTitle` (Türkçe ay adı + yıl) ve `monthlyTotalTime` (`calendar_month_total` + `formatShort`) güncellenir, dekoratörler o aya göre yenilenir. `setOnDateChangedListener` ⇒ yeni `selectedDayText`'te `calendar_day_detail` / `calendar_day_empty`. `CalendarDay` ⇄ anahtar dönüşümü `DateKeys.of/parse` ile (ay 1-tabanlı; 2.0.1 ThreeTen tabanlı). `android.icu.SimpleDateFormat` kullanımı kaldırılır (API 24 uyumsuz risk).
10. **[U3, H18, H14] Ayarlar:** `ChipGroup` (`singleSelection`) 15dk/30dk/1sa/2sa/4sa; chip seçimi EditText'i doldurur (saat cinsinden). Giriş `replace(',', '.')`; `PreferenceHelper.MIN_GOAL_MILLIS..MAX_GOAL_MILLIS` doğrulaması; mevcut değer gösterimi gereksiz ondalıksız (ör. "4", "2.5"). `ViewCompat.setOnApplyWindowInsetsListener` ile insets. Kaydet sonrası Toast'ta `formatGoal`.
11. **[H15, U11-kısmi] Renkler:** `values/colors.xml`'e token'lar (`bg_screen` #F5F7FA, `bg_card` #FFFFFF, `text_primary`, `text_secondary`, `accent` #00B894, `goal_reached`, `goal_missed`, `error`); `values-night/colors.xml` karşılıkları (ör. bg_screen #121212, bg_card #1E1E1E, text_primary #EDEDED). Layout'lardaki tüm sabit renkler token'a çevrilir; drawable'lar da.
12. **[§2.4]** Backend'in ihtiyaç duyduğu 14 string anahtarını **birebir** ekle (isim yazım hatası Faz C'de derlemeyi kırar).
13. **[H19]** UI'daki yerel `formatToShortTime` kopyaları silinir, `TimeFormatter` kullanılır.

---

## 6. Faz D doğrulama görevleri

**Ajan 4 (Kullanışlılık) — kabul edilen U maddeleri kontrol listesi:**
- U1: Geçersiz/erişilemeyen istasyonda ekranda hata metni ve bildirimde `notif_error`; çalma başlarken "Bağlanıyor…", arabellek boşalınca "Arabelleğe alınıyor…"; sayaç bu durumlarda durur.
- U2: 6 istasyonun hepsi listede, aksan etiketiyle; CBC (HLS) çalıyor; liste yalnız `StationRepository`'de.
- U3: Chip'ler hedefi tek dokunuşla ayarlıyor, kaydedince ana ekranda "Hedef:" etiketi güncelleniyor.
- U4: Hedef geçildiğinde günde bir kez kutlama bildirimi; yüzde 100'de kalıyor; "Hedefe ulaşıldı!".
- U5: Tarih kartında seri doğru (bugün dolmadıysa dünden sayıyor).
- U6: Uygulama/servis yeniden açılınca son istasyon adı üst kartta; Toggle son istasyonu çalıyor.
- U7: Takvimde güne dokununca süre; ay değişince başlık + aylık toplam değişiyor.
- U11/U12 (kısmi): karanlık modda metinler okunuyor; ikonlarda contentDescription var; layout'larda sabit metin kalmamış.
- Bulgular Ajan 1'e "Uygulandı / Kısmi / Eksik" etiketiyle raporlanır; düzeltme gerekiyorsa ilgili dosya sahibine iş olarak atanır.

**Ajan 5 (Denetçi):** H1–H25 kabul edilen kısımları kod üzerinden doğrular; özellikle sözleşme uyumu (§2 sabit adları, extra tipleri, 14 string anahtarı), `assembleDebug` başarısı, API 24 uyumluluğu (kanal, registerReceiver, `android.icu` kaldırıldı mı).

---

## 7. Backlog (sonraki turlar)

1. U8 Haftalık istatistik/grafik ekranı.
2. U9 Uyku zamanlayıcı (`ACTION_SLEEP_TIMER`, extra `sleep_minutes`).
3. U10 Kullanıcı istasyon ekleme — Room v2, `Station` tablosu, gerçek `Migration(1,2)`; aynı turda `fallbackToDestructiveMigration` kaldırılır (H17 kalan kısmı), `exportSchema=true`.
4. U11 tam karanlık tema cilası (takvim teması, ikon tint'leri, Material You renkleri).
5. U12 kapsamlı erişilebilirlik testi (TalkBack, dokunma hedefi ≥48dp, font ölçekleme).
6. MediaSessionService / MediaSession geçişi (kilit ekranı ve Bluetooth kontrolleri).
7. Hata sonrası otomatik yeniden deneme (üstel geri çekilme).
8. Media3 sürüm yükseltmesi (1.2.1 → güncel) ve bağımlılık güncellemeleri.

---

## 8. Tur 1b kararları

Tarih: 2026-11-25 · Girdi: Faz D raporları (Denetçi Y1–Y10 + H21/H24 kalanı, Kullanışlılık K1–K9).
**İlke:** Küçük, yerel, davranışı netleştiren düzeltmeler. Durum makinesinin akışını değiştiren işler backlog'a. Dosya sahipliği ve "res/ yalnız UI'da" kuralı §3 ve §2.4'teki gibi geçerli. Tur sonunda `assembleDebug`, `test` ve `lint` yine temiz geçmeli.

### 8.1 Karar tablosu

| # | Konu | Karar | Atanan | Gerekçe / not |
|---|---|---|---|---|
| Y1 (= K6) | Hedef değişince bugünkü `isGoalReached` güncellenmiyor | **KABUL** | Backend (DAO + yardımcı + kalıcı bayrak) + UI (Settings çağrısı) | Takvim ve seri yanlış görünüyor (veri tutarlılığı). Bayrak bir kez `true` olunca geri `false` olmaz (old ‖ new). |
| Y2 | Kısa focus kaybında gereksiz yeniden bağlanma | **BACKLOG** | — | Focus/stale-pause mantığına dokunmak durum makinesini değiştirir; zararı yalnızca kısa bir yeniden arabellekleme. |
| Y3 | Kaynak hazırlanırken anlık sahte PAUSED yayını | **KABUL** | Backend | UI'da titremeye yol açıyor; çözüm yerel bir bastırma bayrağı. |
| Y4 | Yükleme sırasında biriken saniyeler `max` yüzünden kayboluyor | **KABUL** | Backend | Tek satırlık birleştirme düzeltmesi. |
| Y5 | Duraklatılmış servis her saniye uyanıyor | **KABUL** | Backend | Pil; tick yalnız aktif durumlarda çalışır. |
| Y6 | `startForeground` catch'inde `stopSelf` yok | **KABUL** | Backend | Önemsiz değişiklik, yarım kalmış servis bırakmaz. |
| Y7 | Boştaki servis her onResume'da ExoPlayer kuruyor | **KABUL** | Backend | `REQUEST_STATE` hafif kalmalı; player tembel (lazy) kurulur. |
| Y8 | Servis yokken gece yarısı tarih kartı güncellenmiyor | **KABUL** | UI | Activity açıkken gece yarısına zamanlanmış Handler ile çözülür; sistem broadcast'i gerekmez. |
| Y9 (= K5) | Bildirim küçük ikonu ters | **KABUL** | Backend | Kural: küçük ikon **duruma göre değişmez**, sabit `android.R.drawable.ic_media_play` kullanılır. Durum, bildirim metni ve aksiyon düğmesiyle (Duraklat/Devam et) anlatılır. |
| Y10 | Saf yardımcılar için birim testi yok | **KABUL** | Backend (`app/src/test/java/com/example/radio_timer/` Backend'e verildi) | Güvenli; sonraki turlarda regresyonu yakalar. |
| H21 kalanı | `tools:targetApi="31"`, yapay zekâ yorum kalıntıları, kullanılmayan `getAllLogs()` | **KABUL** | Backend | Temizlik. Doğrulandı: `getAllLogs()` hiçbir yerde çağrılmıyor. |
| H24 kalanı | Build küçük kalıntılar | **KOŞULLU KABUL** | Backend | Yalnız `app/build.gradle.kts`/`libs.versions.toml` içinde ve **sürüm yükseltmesi içermiyorsa** yapılır (ör. kullanılmayan katalog girdileri, yorumlar). Sürüm değişikliği gerekiyorsa backlog §7-8. |
| H17 kalanı | Destructive migration | **BACKLOG** | — | §7-3'teki plan değişmedi (U10 ile birlikte). |
| K1 | Chip seçimi ayrıca Kaydet istiyor; kutuda "0.25" | **KABUL** | UI | Chip dokunuşu **anında kaydeder** (Toast + Y1 çağrısı) ve kutuyu doldurur. Kutudaki değer virgülle gösterilir ("0,25", "2,5", tam sayıysa "4"); girişte hem `,` hem `.` kabul edilmeye devam eder. |
| K2 | Hata metni tekrar denemeyi anlatmıyor | **KABUL** | UI | Hata metnine `error_retry_hint` eklenir. Sözleşme gereği ERROR'da karta dokunmak `TOGGLE` ⇒ yeniden prepare (§2.2), yani ipucu doğru. |
| K3 | Hata durumunda istasyon satırı boş | **KABUL** | UI | Hatalı istasyon satırında `station_row_error` gösterilir. |
| K4 | `state_idle` metni akışla çelişiyor | **KABUL** | UI | Yeni metin: "Dinlemeye başlamak için dokun". |
| K7 | Seri bugün tamamlanmadıysa risk göstermiyor | **KABUL** | UI | `streak > 0 && !bugünUlaşıldı` ⇒ `streak_at_risk`. `computeStreak` zaten dünden sayıyor, yeni API gerekmez. |
| K8 | Takvim gün detayında hedef durumu yok; aylık toplamda saniye var | **KABUL** | UI | Gün detayı hedef durumunu gösterir; aylık toplam `TimeFormatter.formatGoal`. |
| K9 | Kayıtlı son istasyon listede değilse ekran ile servis farklı ad gösteriyor | **KABUL** | UI | Ekran adı prefs'teki addan değil, `StationRepository.findByUrl(prefs.getLastStationUrl())` sonucundan alınır, null ise `getDefault()`. Servis de aynı kuralı uyguluyor, böylece ikisi tutarlı olur. |

**Sayılar:** KABUL 17 (Y1, Y3–Y10, H21 kalanı, H24 kalanı (koşullu), K1, K2, K3, K4, K7, K8, K9; K5 ve K6 sırasıyla Y9 ve Y1 ile birleşik). BACKLOG 2 (Y2, H17 kalanı).

### 8.2 Yeni / değişen API (bağlayıcı)

```java
// ListeningDao.java — YENİ. Bayrak yalnızca yükseltilir, asla düşürülmez.
@Query("UPDATE listening_logs SET isGoalReached = 1 WHERE date = :dateKey AND durationMillis >= :goalMillis")
int markGoalReachedIfAtLeast(String dateKey, long goalMillis);

// ListeningDao.java — SİLİNİR: List<ListeningLog> getAllLogs();

// StatsHelper.java — YENİ
@WorkerThread
public static void recomputeGoalFlag(Context context, String dateKey);
    // prefs'teki güncel hedefle markGoalReachedIfAtLeast(dateKey, goal) çağırır.
    // ANA THREAD'DE ÇAĞRILMAZ; UI bunu AppExecutors.diskIO() içinde çağırır.
```

Servis tarafında kalıcı bayrak kuralı (iç davranış, API değil): `saveProgress` diskIO'da mevcut satırı okur ve `isGoalReached = (mevcut != null && mevcut.isGoalReached) || millis >= goal` yazar. Böylece hedef yükseltildiğinde servisin dakikalık kaydı bayrağı geri `false` yapamaz. Bu kural Y1'in zorunlu parçasıdır. Tüm DB işlemleri tek diskIO thread'inde sırayla çalıştığı için yarış oluşmaz.

**Backend yeni string gerektirmiyor.** Broadcast sözleşmesinde (§2.1/§2.2) değişiklik yok.

### 8.3 Backend iş listesi (Ajan 3) — Tur 1b

1. **[Y1]** `ListeningDao.markGoalReachedIfAtLeast`, `StatsHelper.recomputeGoalFlag` (§8.2). `saveProgress` içinde kalıcı bayrak kuralı (old ‖ new).
2. **[Y3]** İstasyon değiştirme / prepare sırasında `isSwitchingSource` (veya eşdeğeri) bayrağı: bu süre içinde `playWhenReady=false` kaynaklı PAUSED yayını bastırılır; yeni kaynak için durum doğrudan `STATE_CONNECTING` olur. Bayrak ilk READY/ERROR'da veya kullanıcı TOGGLE/STOP'unda temizlenir.
3. **[Y4]** Yükleme bitene kadar biriken süre ayrı tutulur (`pendingMillisBeforeLoad`); yükleme bitince `timePlayedToday = dbValue + pendingMillisBeforeLoad` (`max` yerine toplama). Yükleme bitmeden DB'ye yazmama kuralı (§4-14) aynen kalır.
4. **[Y5]** 1 sn'lik tick yalnız `CONNECTING/BUFFERING/PLAYING`'de çalışır; PAUSED/IDLE/ERROR'a geçince `removeCallbacks`. Duraklatmada son kayıt ve son broadcast yapılır. Gün değişimi kontrolü çalma yeniden başlarken de yapılır (H3 korunur).
5. **[Y6]** `startForeground` hatası yakalandığında (ör. `ForegroundServiceStartNotAllowedException`) log + `stopSelf()`; bu durumda çalma başlatılmaz.
6. **[Y7]** `ensurePlayer()`: ExoPlayer yalnız ilk `PLAY_STATION`/`TOGGLE`'da kurulur. `onCreate` ve `REQUEST_STATE` player kurmaz. `onDestroy` ve `STOP` null kontrolüyle release eder.
7. **[Y9]** Oynatma bildirimi küçük ikonu tüm durumlarda sabit `android.R.drawable.ic_media_play`. Kutlama bildirimi (`star_on`) aynen kalır.
8. **[Y10]** `app/src/test/java/com/example/radio_timer/` altında JUnit4 testleri: `TimeFormatterTest` (0, 59 sn, 1 dk, 1 sa 0 dk 5 sn, formatGoal <1 dk ⇒ "0 dk", formatClock), `DateKeysTest` (previousDay ay/yıl/artık yıl sınırı: 2024-03-01 ⇒ 2024-02-29, 2026-01-01 ⇒ 2025-12-31; of/parse gidiş-dönüş), `StatsHelperTest` (progressPercent sınırları: goal≤0, >%100; computeStreak: boş liste, bugün dahil, yalnız dünden, aradaki boşluk). `ExampleUnitTest` silinebilir. `Context` gerektiren metotlar test edilmez.
9. **[H21 kalanı]** Manifest'ten `tools:targetApi="31"` (başka kullanım kalmadıysa `xmlns:tools` de) kaldırılır. `ListeningLog`/`AppDatabase` ve diğer Backend dosyalarındaki sohbet tarzı yorumlar ("Hatanın çözümü", "emin ol" vb.) sade, açıklayıcı yorumlarla değiştirilir veya silinir. `getAllLogs()` silinir.
10. **[H24 kalanı]** Koşullu, §8.1'deki sınırla.

### 8.4 UI iş listesi (Ajan 2) — Tur 1b

1. **[Y1, K1] Ayarlar:** Kaydet ve chip seçimi sonrası `AppExecutors.diskIO().execute(() -> StatsHelper.recomputeGoalFlag(getApplicationContext(), DateKeys.today()))`. Chip dokunuşu anında kaydeder (Toast `formatGoal`), kutuyu doldurur, Activity'yi **kapatmaz**. Kutudaki değer virgüllü gösterilir (tam sayı ⇒ "4", ondalık ⇒ "2,5"/"0,25"; `Locale.US` ile biçimlendirilip `.`→`,`). Giriş `,` ve `.` kabul etmeye devam eder.
2. **[Y8]** MainActivity onResume'da bir sonraki gece yarısına (+1 sn) `Handler.postDelayed`; çalışınca tarih kartı (`currentDateText`), bugünkü ilerleme (DB'den) ve seri yenilenir, sonraki gece yarısı için yeniden zamanlanır. onPause'da `removeCallbacks`. onResume'da ayrıca son gösterilen tarih anahtarı `DateKeys.today()`'den farklıysa hemen yenilenir.
3. **[K2]** Ana ekranda `STATE_ERROR`: `error_*` metni + `error_retry_hint`.
4. **[K3]** `StationAdapter`: hatalı satırın (aktif URL + `STATE_ERROR`) `stationTimer` alanında `station_row_error`, hata rengiyle.
5. **[K4]** `state_idle` metni güncellenir (§8.5).
6. **[K7]** Seri metni: `streak == 0` ⇒ `streak_none`; `streak > 0` ve bugün ulaşılmadıysa ⇒ `streak_at_risk`; ulaşıldıysa ⇒ `streak_label`. "Bugün ulaşıldı" bilgisi broadcast'teki `EXTRA_GOAL_REACHED`'ten, broadcast yoksa DB'deki bugünkü satırdan (`isGoalReached` veya `durationMillis >= goal`) alınır.
7. **[K8]** Takvim gün detayı: kayıt var ve `isGoalReached` ⇒ `calendar_day_detail_reached`; gün bugün ve ulaşılmadıysa ⇒ `calendar_day_detail_today`; geçmiş gün ve ulaşılmadıysa ⇒ `calendar_day_detail_missed`; kayıt yoksa ⇒ `calendar_day_empty` (mevcut). Süre `formatShort`. Aylık toplam `TimeFormatter.formatGoal`.
8. **[K9]** Üst karttaki ilk istasyon adı ve adapter'ın ilk aktif URL'si: `Station s = StationRepository.findByUrl(prefs.getLastStationUrl()); if (s == null) s = StationRepository.getDefault();` ⇒ `s.getName()`, `s.getUrl()`. Ekranda artık `getLastStationName()` kullanılmaz.

### 8.5 UI'ın ekleyeceği / değiştireceği string'ler

| Anahtar | Metin | Durum |
|---|---|---|
| `state_idle` | Dinlemeye başlamak için dokun | DEĞİŞİR |
| `error_retry_hint` | Tekrar denemek için karta dokunun. | YENİ |
| `station_row_error` | Hata | YENİ |
| `streak_at_risk` | Seri: %1$d gün · devam etmek için bugünü tamamla | YENİ |
| `calendar_day_detail_reached` | %1$s: %2$s · hedef tuttu | YENİ |
| `calendar_day_detail_missed` | %1$s: %2$s · hedef tutmadı | YENİ |
| `calendar_day_detail_today` | %1$s: %2$s · devam ediyor | YENİ |
| `calendar_day_detail` | — | Artık kullanılmıyorsa silinir (lint `UnusedResources` uyarısı vermesin) |

### 8.6 Doğrulama (Tur 1b sonu)

- **Ajan 5:** Y1 senaryosu: bugün 1 sa dinlenmiş ve hedef 30 dk ⇒ bayrak true. Hedef 2 sa'e yükseltilir ⇒ bayrak true kalır, bir dakikalık servis kaydından sonra da true. Başka bir günde hedef düşürülünce bayrak false'tan true'ya döner. Ayrıca Y3–Y7 kod incelemesi ve `gradlew assembleDebug test lint` temiz.
- **Ajan 4:** K1 (chip anında kaydediyor, virgüllü gösterim), K2/K3 (geçersiz istasyonda ipucu + satırda "Hata"), K4, K7, K8, K9, Y9 ikonu.
- **Ajan 1:** CLAUDE.md değişiklik günlüğüne Tur 1b satırı; "Bilinen sorunlar"a backlog'daki Y2 eklenir.

### 8.7 Backlog'a eklenenler

9. Y2: Kısa (transient) focus kaybında yeniden bağlanmayı önle (`STALE_PAUSE_MS` mantığını focus kaybı nedenine göre ayır).
