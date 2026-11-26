# Radio_Timer — Tur 2 Karar Dokümanı

Tarih: 2026-11-25 · Hazırlayan: Ajan 1 (Proje Yöneticisi)
Girdi: Ajan 4 önerileri (B1–B4, Y1-hatırlatma), Ajan 5 denetimi (T1–T11 + teknik sıra önerisi), güncel CLAUDE.md, `docs/tur1-kararlar.md`.
Önceki sözleşme (`docs/tur1-kararlar.md` §2, §8.2) geçerliliğini korur. Bu doküman yalnızca **eklemeleri ve değişiklikleri** tanımlar.

**Tur 2 teması: "Kararlılık + bağımlılık sadeleştirme + uyku zamanlayıcı".**
Sınırlar: Room şeması değişmez (v1; yalnız `exportSchema` hazırlığı), Media3 1.2.1'de kalır, MediaSession yok, yeni izin yok. Tur sonunda `./gradlew assembleDebug testDebugUnitTest lintDebug` temiz geçmeli.

> Not: Ajan 4'ün "Y1 günlük hatırlatma" önerisi, Tur 1b'deki Y1 (hedef bayrağı) ile karışmasın diye bu dokümanda **R1** olarak anılır.

---

## 1. Çelişkilerin çözümü

### 1.1 Otomatik yeniden deneme (B2) — Ajan 4 "Tur 2" / Ajan 5 "MediaSession sonrası"
**Karar: Ajan 5 haklı. B2 BACKLOG, Tur 5'e (MediaSessionService turu) gidiyor.**
- MediaSessionService geçişinde oynatma durum makinesi ve foreground yaşam döngüsü yeniden yazılacak. Retry/backoff, NetworkCallback ve `state_retrying` durumunu şimdi eklemek o turda iki kez yazılmasına ve iki kez test edilmesine yol açar.
- Durum makinesine yeni dallar eklemek (zamanlayıcı, ağ geri çağrısı, iptal kuralları) Tur 2'nin "güvenli" sınırını aşar.
- B2'nin **Y2/focus kısmı ayrılıyor ve bu tur T3 olarak uygulanıyor.** Böylece kullanıcının en sık yaşadığı sorun (telefon/navigasyon sonrası gereksiz yeniden bağlanma ve yanlış "duraklatıldı" görünümü) bu tur çözülüyor.
- Ara çözüm: hata ekranındaki "Tekrar denemek için karta dokunun" ipucu (Tur 1b) yeterli.

### 1.2 Takvim dili (B1 takvim kısmı) ↔ kendi takvim ızgarası (T4)
**Karar: T4 seçildi. B1'in takvim kısmı T4 içinde çözülüyor, ayrıca uygulanmayacak.**
- material-calendarview 2018'den beri bakımsız; jitpack, Jetifier ve ThreeTen gerektiriyor. Kütüphaneye Türkçe yerelleştirme yamamak, birkaç tur sonra atılacak bir koda emek harcamak olur.
- Kendi ızgaramız (RecyclerView + GridLayoutManager, 7 sütun) Türkçe ay/gün adlarını, Pazartesi başlangıcını ve karanlık mod uyumunu doğal olarak sağlıyor. Ayrıca jitpack/Jetifier/threetenabp/RadioTimerApp kaldırılarak build sadeleşiyor.
- Izgara hesabı saf bir yardımcıya (`CalendarMath`, Backend) taşınıyor ve birim testle korunuyor. UI yalnızca adapter ve görünümü yazıyor.
- B1'in **liste sıkılaştırma kısmı** ayrı bir UI işi olarak kabul edildi.

---

## 2. Kararlar

| # | Konu | Efor | Karar | Atanan | Gerekçe |
|---|---|---|---|---|---|
| B1-liste | İstasyon listesi sıkılaştırma (padding, ikon, fading edge) | S | **KABUL** | UI | Saf görsel iş, risk yok. |
| B1-takvim | Takvim Türkçe gün/ay, Pazartesi başlangıç | S | **KABUL (T4 içinde)** | UI + Backend | §1.2. |
| B2 | Otomatik yeniden deneme + NetworkCallback | M | **BACKLOG → Tur 5** | — | §1.1. Y2 kısmı T3'te. |
| B3 | Uyku zamanlayıcı (U9) | S-M | **KABUL** | Backend + UI | Önceki turdan beri bekleyen, değeri yüksek bir istek. Durum makinesine dokunmuyor: ayrı bir Handler ve bitince mevcut `handleStop`. |
| B4 | Haftalık özet kartı (U8) | M | **BACKLOG → Tur 3** | — | Takvim ekranı bu tur T4 ile baştan yazılıyor; aynı dosyaya ikinci büyük iş eklemek çakışma ve regresyon riski taşıyor. Tur 3'te yeni ızgaranın üstüne eklenecek. |
| R1 | Günlük "bugün henüz dinlemedin" hatırlatması | M | **BACKLOG → Tur 3** | — | AlarmManager, BOOT_COMPLETED ve yeni receiver/izin gerektiriyor. Tur 3 "özellik turu"nda B4 ile birlikte yapılacak. |
| T1 | Bildirim izni yoksa kalıcı uyarı satırı | S | **KABUL** | UI (+ Backend prefs) | Android 13+'ta izin reddedilince oynatma kontrolleri görünmüyor. Kısa vadeli çözüm bu, kalıcı çözüm Tur 5. T10 ile birleşik. |
| T2 | Process ölümünde ≤60 sn kayıp | S | **KABUL** | Backend | Kayıt aralığı 15 sn + `onTaskRemoved` kaydı. Ucuz bir veri güvencesi. |
| T3 | Y2 + sözleşme sapması (kısa focus kaybında `is_playing=false`) | M | **KABUL** | Backend (+ UI yeni durum metni) | Tur 1 sözleşmesine (`is_playing` = playWhenReady) uyum sağlıyor. Karar mantığı saf fonksiyonda ve testli. Yeni durum: `STATE_INTERRUPTED` (§3.1). |
| T4 | Kendi ay ızgarası; kütüphane/jitpack/Jetifier/ThreeTen kaldırma | M | **KABUL** | UI (ekran) + Backend (CalendarMath, build) | §1.2. |
| T5 | `tickRunnable` koşulsuz yeniden kurulum; eski `date_key` | S | **KABUL** | Backend (tick) + UI (eski anahtarı yok say) | Küçük bir sağlamlık düzeltmesi. |
| T6 | Duraklatılmış servis süresiz foreground (DETACH) | S | **BACKLOG → Tur 5** | — | DETACH sonrası sistem servisi öldürebilir; kalan bildirim ve yeniden başlatma akışı ayrıca tasarlanmalı. MediaSessionService bu yaşam döngüsünü zaten yönetiyor. Uyku zamanlayıcı bitince servis tamamen durduğu için (B3) en sık senaryo bu tur çözülüyor. |
| T7 | Hedef düşürülünce kutlama yok | S | **KABUL** | Backend | Koşul: `timePlayed >= goal && lastCelebrated != today`. |
| T8 | Döndürmede chip listener'ının tekrar tetiklenmesi (şüpheli) | S | **KABUL** | UI | Programatik `check` sırasında kaydetmeyi engelleyen bir bayrak. Ucuz bir önlem. |
| T9 | URL'siz `PLAY_STATION` | S | **KABUL** | Backend | §3.2 kuralı. |
| T10 | İzin gerekçe diyaloğu her açılışta | S | **KABUL** | UI + Backend (prefs) | İzin yalnız bir kez sorulur, sonrası T1 satırıyla yönetilir. |
| T11 | Release'de R8 kapalı | S | **BACKLOG → Tur 4** | — | Media3 yükseltmesiyle birlikte ele alınacak (keep kuralları o sürüme göre). |
| Teknik-1 | Room `exportSchema=true` + `app/schemas/.../1.json` | S | **KABUL** | Backend | Şema değişmiyor. Tur 6 migration'ı için zorunlu bir hazırlık, risksiz. |
| Teknik-2 | Media3 1.2.1 → 1.9.4 | M | **BACKLOG → Tur 4** | — | Ayrı ve izole bir bağımlılık turu. |
| Teknik-3 | MediaSessionService | L | **BACKLOG → Tur 5** | — | — |
| Teknik-5 | Room v2 + U10 (@AutoMigration, room-testing) | L | **BACKLOG → Tur 6** | — | — |

**Sayılar:** Tur 2 kapsamında 13 kalem KABUL: B1-liste, B1-takvim (T4 içinde), B3, T1, T2, T3, T4, T5, T7, T8, T9, T10, Teknik-1 (exportSchema). BACKLOG'a 8 kalem gidiyor: B2, B4, R1, T6, T11, Media3, MediaSession, Room v2/U10. RET yok.

---

## 3. Sözleşme eklemeleri (bağlayıcı)

### 3.1 Broadcast (`ACTION_STATE_UPDATE`) — eklemeler / değişiklikler

| Değişiklik | Ayrıntı |
|---|---|
| **YENİ durum değeri** | `public static final String STATE_INTERRUPTED = "interrupted";` — playWhenReady **true** ama oynatma geçici olarak bastırılmış (`getPlaybackSuppressionReason() != NONE`; ör. kısa süreli audio focus kaybı, telefon görüşmesi, navigasyon anonsu). Sayaç **ilerlemez**. `is_playing = true` (UI duraklat ikonu gösterir; toggle duraklatır). |
| **Netleştirme** | `EXTRA_IS_PLAYING` artık kesin olarak `playWhenReady` demek: CONNECTING/BUFFERING/PLAYING/INTERRUPTED ⇒ true; PAUSED/IDLE/ERROR ⇒ false. Bastırma, `is_playing`'i false yapmaz (T3 sapması giderilir). |
| **YENİ extra** | `public static final String EXTRA_SLEEP_REMAINING_MS = "sleep_remaining_ms";` — `long`. Uyku zamanlayıcı yoksa `0`, varsa kalan ms (>0). Her broadcast'te gönderilir. Not: duraklatılmışken tick yok (Tur 1b Y5), bu yüzden değer yalnız durum değişiminde ve `REQUEST_STATE`'te güncellenir. UI dakika düzeyinde gösterir; duraklatılmışken bayat değer kabul edilir. |
| **Sıralama kuralı** | `date_key` sözlük sırasıyla karşılaştırılabilir ("yyyy-MM-dd"). UI, son gösterdiği anahtardan **küçük** bir `date_key` taşıyan broadcast'i yok sayar (T5). |

Durum çözümleme önceliği (Backend'in `PlaybackDecisions.resolveState` ile uygulayacağı, test edilen kural):
1. Medya yok (hiç istasyon hazırlanmadı / STOP sonrası) ⇒ `STATE_IDLE`
2. Hata var (`onPlayerError` sonrası, yeniden prepare edilene kadar) ⇒ `STATE_ERROR`
3. Kaynak değiştiriliyor (Tur 1b Y3 bayrağı) ⇒ `STATE_CONNECTING`
4. `!playWhenReady` ⇒ `STATE_PAUSED`
5. Exo `STATE_ENDED` (canlı yayın kesildi) ⇒ `STATE_ERROR` (servis `error_code = ERROR_SOURCE` atar)
6. Bastırma var ⇒ `STATE_INTERRUPTED`
7. Exo `STATE_BUFFERING` veya `STATE_IDLE`: son prepare'den beri en az bir kez READY olduysa ⇒ `STATE_BUFFERING`, olmadıysa ⇒ `STATE_CONNECTING`
8. Exo `STATE_READY` ⇒ `STATE_PLAYING`

### 3.2 Servise giden action'lar — eklemeler / değişiklikler

```java
public static final String ACTION_SLEEP_TIMER  = "com.example.radio_timer.action.SLEEP_TIMER";
public static final String EXTRA_SLEEP_MINUTES = "sleep_minutes"; // int
```

| Action | Extra | UI nasıl gönderir | Servis davranışı |
|---|---|---|---|
| `ACTION_SLEEP_TIMER` | `EXTRA_SLEEP_MINUTES` (`int`; `0` = iptal; UI yalnız 15/30/45/60 gönderir, servis 1..180 aralığına sıkıştırır) | `startService`, `try/catch(IllegalStateException)` içinde. **Yalnız `player_state != idle` iken** (servis zaten foreground'da çalışıyor). | Aktif oturum yoksa (IDLE) yok sayar ve `REQUEST_STATE` gibi kapanır (`stopSelfResult`, startForeground yok). Varsa: bitiş zamanı `SystemClock.elapsedRealtime()` ile tutulur. Zamanlayıcı **duvar saatiyle** işler, duraklatma onu durdurmaz. Bitişten 30 sn önce ses lineer olarak 1.0'dan 0'a kısılır (`exoPlayer.setVolume`). Bitişte **`handleStop()`** çalışır: kayıt, IDLE, bildirim kaldırma, `stopSelf`. Ses seviyesi iptalde, STOP'ta ve yeni `PLAY_STATION`'da 1.0'a döner. `ACTION_STOP` zamanlayıcıyı da iptal eder. Ayar ve iptal sonrası hemen broadcast yapılır. |
| `ACTION_PLAY_STATION` (değişiklik, T9) | URL veya ad eksik/boş | — | Önce koşulsuz `startForeground` (Tur 1 H1 kuralı). Sonra: aktif oturum yoksa `handleStop()`, varsa isteği yok sayıp mevcut durumu yayınlar. Log uyarısı. |
| `ACTION_TOGGLE` (değişiklik, T3) | — | aynı | Karar `PlaybackDecisions.decideToggle(...)` ile verilir (§3.3). Özetle: playWhenReady true (INTERRUPTED dahil) ⇒ PAUSE. ERROR/IDLE ⇒ REPREPARE. Kullanıcının kendisi duraklattıysa ve `STALE_PAUSE_MS`'ten uzun süre geçtiyse ⇒ REPREPARE, değilse ⇒ RESUME. Kısa focus kaybı artık `pausedAtElapsed` damgası **basmaz**; damga yalnız playWhenReady true→false geçişinde basılır. |

### 3.3 Yeni / değişen sınıflar ve imzalar (Backend yazar)

```java
// PlaybackDecisions.java — YENİ, saf (Android Context yok), birim testli
public final class PlaybackDecisions {
    public static final long STALE_PAUSE_MS = 30_000L;   // RadioService'teki sabit buraya taşınır
    public enum ToggleAction { PAUSE, RESUME, REPREPARE }

    public static boolean isSuppressed(int playbackSuppressionReason);
        // reason != Player.PLAYBACK_SUPPRESSION_REASON_NONE

    public static String resolveState(boolean hasMedia, boolean hasError, boolean switchingSource,
                                      boolean playWhenReady, int playbackSuppressionReason,
                                      int exoPlaybackState, boolean readySincePrepare);
        // §3.1 öncelik listesi; RadioService.STATE_* değerlerinden birini döndürür, asla null

    public static ToggleAction decideToggle(String playerState, boolean playWhenReady,
                                            long pausedAtElapsed, long nowElapsed);
        // ERROR/IDLE ⇒ REPREPARE; playWhenReady ⇒ PAUSE;
        // !playWhenReady && pausedAtElapsed > 0 && now - pausedAt > STALE_PAUSE_MS ⇒ REPREPARE; aksi ⇒ RESUME

    public static boolean counterShouldRun(String playerState); // yalnız STATE_PLAYING ⇒ true
}

// CalendarMath.java — YENİ, saf, birim testli (java.util.GregorianCalendar; java.time KULLANILMAZ: minSdk 24)
public final class CalendarMath {
    public static int daysInMonth(int year, int month1to12);
    public static List<String> monthCells(int year, int month1to12);
        // Pazartesi başlangıçlı ızgara hücreleri: ayın 1'inden önceki boşluklar null,
        // ardından DateKeys formatında "yyyy-MM-dd" günler, son satır null ile 7'ye tamamlanır.
        // Uzunluk 7'nin katı (28..42). Ör. 2026-11 (1 Kasım = Pazar) ⇒ 6 null + 30 gün + 6 null = 42.
    public static int[] addMonths(int year, int month1to12, int delta); // {year, month1to12}
    public static int[] currentYearMonth();                             // {year, month1to12}, cihaz saat dilimi
}

// PreferenceHelper.java — EKLEMELER (T10)
public boolean wasNotificationPermissionAsked();
public void    setNotificationPermissionAsked();
```

`RadioService` iç kuralları (API değil ama doğrulanacak):
- `SAVE_INTERVAL_MS = 15_000L` (T2). `onTaskRemoved(Intent)` ⇒ kaydedilmemiş süre varsa hemen `saveProgress` (diskIO). `onDestroy`'da da aynı şekilde.
- Tick (T5): `tickRunnable` kendini yalnız `tickRunning == true` ise yeniden kurar; `stopTick()` bayrağı indirip `removeCallbacks` yapar.
- Kutlama (T7): hem tick'te hem hedef değişikliği algılandığında (her broadcast öncesi prefs'ten okunan hedef, son bilinen hedeften farklıysa) koşul `timePlayedToday >= goal && !today.equals(lastCelebratedDate)`. Koşul sağlanırsa kutlama bildirimi + `setLastCelebratedDate(today)` + `markGoalReachedIfAtLeast`.
- Bildirim: INTERRUPTED'da metin `notif_interrupted`; uyku zamanlayıcı aktifken `setSubText(getString(R.string.notif_sleep_sub, TimeFormatter.formatGoal(kalan)))`. Notify yalnız metin değişince yapılır (Tur 1 H20 kuralı); bu yüzden alt metin dakika granülerliğinde.
- `RadioTimerApp.java` **silinir** (yalnız ThreeTen init içeriyordu); manifest'teki `android:name=".RadioTimerApp"` kaldırılır.

### 3.4 String kaynakları (res/ yalnız UI'da)

**Backend'in kullandığı, UI'ın birebir eklemesi ZORUNLU olanlar:**

| Anahtar | Metin |
|---|---|
| `notif_interrupted` | Başka bir uygulama sesi kullanıyor |
| `notif_sleep_sub` | Uyku: %1$s |

**UI'ın kendi kullanımı için eklediği anahtarlar (adlar bağlayıcı, metinler önerilen):**

| Anahtar | Metin |
|---|---|
| `state_interrupted` | Başka bir uygulama sesi kullanıyor |
| `sleep_dialog_title` | Uyku zamanlayıcı |
| `sleep_option_minutes` | %1$d dakika |
| `sleep_cancel` | Zamanlayıcıyı kapat |
| `sleep_remaining` | Uyku: %1$s |
| `sleep_set_toast` | Radyo %1$d dakika sonra kapanacak |
| `sleep_cancelled_toast` | Uyku zamanlayıcı kapatıldı |
| `cd_sleep_timer` | Uyku zamanlayıcı |
| `notif_permission_warning` | Bildirimler kapalı, oynatma kontrolleri görünmüyor. Açmak için dokun. |
| `calendar_month_names` (string-array) | Ocak, Şubat, Mart, Nisan, Mayıs, Haziran, Temmuz, Ağustos, Eylül, Ekim, Kasım, Aralık |
| `calendar_weekdays_short` (string-array, Pazartesi'den) | Pzt, Sal, Çar, Per, Cum, Cmt, Paz |
| `calendar_header` | %1$s %2$d *(ay adı + yıl)* |
| `cd_prev_month` | Önceki ay |
| `cd_next_month` | Sonraki ay |

Kullanılmayan eski izin diyaloğu metinleri (gerekçe başlığı/mesajı, "İzin Ver"/"Vazgeç") T10 sonrası kullanılmıyorsa silinir; lint `UnusedResources` temiz kalmalı.

---

## 4. Dosya sahipliği (Tur 2)

| Ajan | Dosyalar |
|---|---|
| **Backend (Ajan 3)** | `RadioService.java`, `PreferenceHelper.java`, `AppDatabase.java`, `ListeningDao.java`, `ListeningLog.java`, `Station*.java`, `StatsHelper.java`, `TimeFormatter.java`, `DateKeys.java`, `AppExecutors.java`, **yeni:** `PlaybackDecisions.java`, `CalendarMath.java`; **silinecek:** `RadioTimerApp.java`; `app/build.gradle.kts`, `gradle/libs.versions.toml`, **bu tur ek olarak** `settings.gradle.kts`, `gradle.properties`, `app/schemas/**`; `AndroidManifest.xml`; `app/src/test/**` |
| **UI (Ajan 2)** | `MainActivity.java`, `SettingsActivity.java`, `CalendarActivity.java`, `StationAdapter.java`, **yeni:** `MonthGridAdapter.java`; `res/**` |
| **Ajan 4 / Ajan 5** | Faz D doğrulama (§7). Dosya yazmazlar. |
| **Ajan 1** | `CLAUDE.md`, `docs/**` |

Entegrasyon notu: Backend takvim kütüphanesini build'den kaldırdığında, UI `CalendarActivity`'yi yeniden yazmadan derleme kırılır. Bu beklenen bir durum: tam derleme Faz C'de, iki taraf da bitince yapılır.

---

## 5. Backend iş listesi (Ajan 3)

1. **[T3] `PlaybackDecisions`** (§3.3). `RadioService` durum hesaplamasını `resolveState` ile, toggle'ı `decideToggle` ile, sayaç koşulunu `counterShouldRun` ile yap. `STATE_INTERRUPTED` sabitini ekle. `onPlaybackSuppressionReasonChanged` ⇒ durum yeniden çözülür ve yayınlanır. `pausedAtElapsed` yalnız playWhenReady true→false geçişinde basılır. INTERRUPTED'da `is_playing=true`.
2. **[T3, test]** `PlaybackDecisionsTest`: resolveState'in 8 öncelik dalı (en az birer test; bastırma + buffering birlikteyken INTERRUPTED; ENDED ⇒ ERROR), decideToggle'ın 4 sonucu (kısa focus kaybı sonrası RESUME, 31 sn kullanıcı duraklatması sonrası REPREPARE, INTERRUPTED'da PAUSE, ERROR'da REPREPARE), counterShouldRun.
3. **[B3] Uyku zamanlayıcı** (§3.2): `ACTION_SLEEP_TIMER`, `EXTRA_SLEEP_MINUTES`, `EXTRA_SLEEP_REMAINING_MS`. Ayrı bir `Handler` ile iki zamanlanmış iş: ses kısma başlangıcı (bitiş − 30 sn; kısma ~1 sn adımlarla) ve bitiş (`handleStop`). İptal, STOP ve yeni istasyonda ses 1.0'a döner. Bildirim alt metni `notif_sleep_sub`.
4. **[T2]** `SAVE_INTERVAL_MS = 15_000L`; `onTaskRemoved` + `onDestroy`'da kaydedilmemiş süreyi kaydet.
5. **[T5]** Tick'in kendini yeniden kurması `tickRunning` koşuluna bağlanır.
6. **[T7]** Kutlama koşulu (§3.3), hedef düşürmeyi de kapsar.
7. **[T9]** URL'siz/adsız `PLAY_STATION` kuralı (§3.2).
8. **[T10]** `PreferenceHelper.wasNotificationPermissionAsked / setNotificationPermissionAsked`.
9. **[T4] `CalendarMath`** (§3.3) + `CalendarMathTest`: 2026-11 (Pazar başlangıç ⇒ 6 önde boşluk), Pazartesi ile başlayan bir ay (ör. 2026-06 ⇒ 0 boşluk), Şubat 2024 (29 gün) / 2026 (28), `addMonths` yıl sınırları (2026-12 +1 ⇒ 2027-01; 2026-01 −1 ⇒ 2025-12; ±13), uzunluğun 7'nin katı olması.
10. **[T4] Build sadeleştirme:** `material-calendarview` ve `threetenabp` bağımlılıklarını ve katalog girdilerini kaldır. Başka bağımlılık kullanmıyorsa `settings.gradle.kts`'ten jitpack deposunu kaldır. `gradle.properties`'ten `android.enableJetifier=true` satırını kaldır. `RadioTimerApp.java`'yı sil ve manifest'ten `android:name`'i kaldır. Doğrulama: `./gradlew :app:dependencies --configuration debugRuntimeClasspath` çıktısında `com.android.support` kalmamalı.
11. **[Teknik-1] Room şema dışa aktarımı:** `@Database(..., version = 1, exportSchema = true)`. `defaultConfig { javaCompileOptions { annotationProcessorOptions { arguments["room.schemaLocation"] = "$projectDir/schemas" } } }`. Üretilen `app/schemas/com.example.radio_timer.AppDatabase/1.json` repoya eklenir. Versiyon ve tablo **değişmez**; `fallbackToDestructiveMigration` Tur 6'ya kadar kalır.
12. Sürüm yükseltmesi YOK (Media3, Room, AGP, compileSdk aynı kalır).

## 6. UI iş listesi (Ajan 2)

1. **[T4, B1-takvim] Takvim ekranı yeniden yazılır:** `activity_calendar.xml`'den `MaterialCalendarView` kaldırılır. Yerine: başlık satırı (önceki ay oku, `calendar_header` ile "Kasım 2026", sonraki ay oku; ikonlarda `cd_prev_month/next_month`), `calendar_weekdays_short` ile 7'li gün başlığı, `RecyclerView` (`GridLayoutManager(7)`, `calendarGrid`, iç içe kaydırma kapalı). Yeni `MonthGridAdapter` + `item_calendar_day.xml`. Hücreler `CalendarMath.monthCells(y, m)` ile oluşur, null hücre boş ve tıklanamaz.
   - Boyama Tur 1 kurallarıyla aynı: `isGoalReached` ⇒ yeşil daire, geçmiş gün ve ulaşılmadıysa ⇒ kırmızı daire, bugün ⇒ yalnız çerçeve halkası (dolmadıysa), gelecek ⇒ soluk metin. Renkler token'lardan, karanlık modda okunur.
   - Güne dokunma: Tur 1b K8 detay metinleri. Ay değişimi: başlık + `getLogsForMonth` + aylık toplam. Ay/yıl `savedInstanceState`'te korunur.
   - `com.prolificinteractive` ve `org.threeten` import'u hiçbir dosyada kalmaz; `today_selector` gibi kullanılmayan drawable'lar silinir.
2. **[B1-liste]** `item_station.xml`: dikey padding ve ikon boyutu küçültülür (dokunma hedefi ≥48dp korunur). RecyclerView'a `requiresFadingEdge="vertical"`. 6 istasyon tipik telefonda kaydırmadan veya az kaydırmayla görünür.
3. **[B3] Uyku zamanlayıcı UI:** üst karta ay ikonu (`cd_sleep_timer`, vektör drawable). Yalnız `player_state != idle` iken etkin. Dokununca `sleep_dialog_title` başlıklı tek seçimli liste: 15/30/45/60 dakika (`sleep_option_minutes`), zamanlayıcı aktifse ek olarak `sleep_cancel`. Seçim `ACTION_SLEEP_TIMER` + `EXTRA_SLEEP_MINUTES` ile gönderilir (`startService`, try/catch) ve ardından `sleep_set_toast` / `sleep_cancelled_toast` gösterilir. `EXTRA_SLEEP_REMAINING_MS > 0` ise ikonun yanında `sleep_remaining` + `TimeFormatter.formatGoal`, 0 ise gizli.
4. **[T3]** `STATE_INTERRUPTED` ⇒ durum metni `state_interrupted`, ikon `is_playing`'e göre (duraklat). Adapter'da bu durum PLAYING gibi ele alınır (satırda hata/bağlanıyor göstergesi yok). Bilinmeyen bir `player_state` gelirse `state_idle` metni (ileriye dönük güvenlik).
5. **[T1, T10] Bildirim izni:**
   - Açılışta izin yoksa ve `!prefs.wasNotificationPermissionAsked()` ise (API 33+) izin **gerekçe diyaloğu olmadan** doğrudan istenir, `setNotificationPermissionAsked()` çağrılır.
   - Bildirimler kapalıysa (`NotificationManagerCompat.from(this).areNotificationsEnabled() == false`, tüm API'lerde) ana ekranda kalıcı uyarı satırı (`notif_permission_warning`, uyarı rengi) görünür. Dokununca API 26+ için `Settings.ACTION_APP_NOTIFICATION_SETTINGS` (+ `EXTRA_APP_PACKAGE`), altı için `ACTION_APPLICATION_DETAILS_SETTINGS` açılır.
   - Satır onResume'da yeniden değerlendirilir. Eski gerekçe diyaloğu ve reddedilince çıkan Toast kaldırılır.
6. **[T5]** Alıcıda, `date_key` son gösterilen anahtardan küçükse broadcast yok sayılır.
7. **[T8]** SettingsActivity: chip'ler programatik olarak işaretlenirken (onCreate/geri yükleme) kaydetme tetiklenmez. Bunun için bir `suppressChipSave` bayrağı kullanılır veya listener işaretleme yapıldıktan sonra bağlanır.
8. §3.4'teki string'ler (Backend'in iki zorunlu anahtarı birebir); kullanılmayan kaynaklar temizlenir.

---

## 7. Faz D doğrulama

**Ajan 4 (Kullanışlılık):**
- Takvim: Türkçe ay/gün adları, Pazartesi başlangıç, ay geçişi, gün detayı, karanlık mod, döndürmede ayın korunması.
- Liste: 6 istasyonun sığması, dokunma hedefleri.
- Uyku zamanlayıcı: 15 dk seçimi, kalan sürenin görünmesi, iptal, son 30 sn'de sesin kısılması (test için geçici olarak kısa süre değil, kod incelemesi + 15 dk gerçek deneme veya emülatörde saat ileri alma), bitince bildirimin kaybolması ve sayacın durması.
- Bildirim izni reddedilince uyarı satırı, dokununca ayarlar ekranı; diyaloğun her açılışta çıkmaması.
- Kısa focus kaybı (emülatörde başka bir uygulamadan kısa ses / `adb shell cmd media_session` ile) sonrası "Başka bir uygulama sesi kullanıyor" metni ve otomatik devam; yeniden bağlanma olmaması.

**Ajan 5 (Denetçi):**
- §3 sözleşmesine uyum (`STATE_INTERRUPTED`, `EXTRA_SLEEP_*`, `ACTION_SLEEP_TIMER`, iki zorunlu string).
- `PlaybackDecisionsTest` ve `CalendarMathTest` kapsamı. Toplam test sayısının artması (26'dan fazla).
- Bağımlılık ağacında `com.android.support`, jitpack, threeten kalmaması; Jetifier kapalı.
- `app/schemas/.../1.json` mevcut ve DB versiyonu 1.
- T2/T5/T7/T9 kod incelemesi; API 24 uyumu (java.time kullanılmamış); `assembleDebug testDebugUnitTest lintDebug` temiz.

**Ajan 1:** CLAUDE.md güncellenir: dosya haritası (`PlaybackDecisions`, `CalendarMath`, `MonthGridAdapter` eklenir; `RadioTimerApp` çıkar), teknoloji (calendarview/jitpack/Jetifier/ThreeTen çıkar), sözleşme özeti (`interrupted`, uyku zamanlayıcı), backlog ve yol haritası.

---

## 8. Yol haritası (sonraki turlar)

| Tur | Tema | Kapsam | Ön koşul / not |
|---|---|---|---|
| **Tur 2** (bu tur) | Kararlılık + sadeleştirme + uyku zamanlayıcı | §2'deki KABUL'ler | Şema yok, bağımlılık yükseltmesi yok |
| **Tur 3** | Özellik turu (şemasız) | B4 haftalık özet kartı (DAO `getLogsBetween`, `StatsHelper.weekSummary` saf fonksiyon + test, `DateKeys.weekStart`; yeni takvim ekranının üstünde 7 çubuk + toplam/ortalama/x-7 gün/geçen haftaya göre %), R1 günlük hatırlatma (AlarmManager inexact, `BOOT_COMPLETED`, `ReminderReceiver`, prefs `reminderEnabled`/`reminderTime` 19:00, ayarlarda switch + TimePicker), hafta sonu için farklı hedef (yalnız prefs) | DAO'ya yalnız sorgu eklenir, şema değişmez |
| **Tur 4** | Bağımlılık turu | Media3 1.2.1 → 1.9.4 (compileSdk 35 ile uyumlu en yüksek; 1.10+ compileSdk 36 ister), release'de R8 açılır + keep kuralları (T11), diğer androidx güncellemeleri | Yalnız yükseltme; davranış değişikliği yok, regresyon testi emülatörde |
| **Tur 5** | MediaSession turu | `MediaSessionService` geçişi (kilit ekranı, Bluetooth/kulaklık düğmeleri, izin reddinde de kontroller ⇒ T1 kalıcı çözüm), foreground yaşam döngüsü (T6), durum makinesinin session üzerine yeniden kurulması, **B2 otomatik yeniden deneme** (ERROR_NETWORK'te üstel 1–30 sn, NetworkCallback, `STATE_RETRYING`, `EXTRA_RETRY_IN_MS`) | Tur 4 (Media3 güncel) tamamlanmış olmalı |
| **Tur 6** | Şema turu (Room v2) | U10 istasyon ekleme/favoriler, aksan bazlı istatistik tablosu, kelime notu; tek `Migration(1,2)` / `@AutoMigration`, `room-testing` ile migration testi, `fallbackToDestructiveMigration` kaldırılır | Tur 2'deki `1.json` şema dışa aktarımı ön koşuldur |
| Sürekli | — | U11 tam tema cilası, U12 erişilebilirlik testi, gerçek cihaz/Android 7.x denemesi, Avustralya/YZ istasyonu araştırması | Uygun turlara eklenir |
