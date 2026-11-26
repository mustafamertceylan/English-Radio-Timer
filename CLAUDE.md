# Radio_Timer — Proje Hatırlatıcısı

## Amaç
İngilizce öğrenmek için İngilizce radyo istasyonlarını dinleten ve kullanıcının belirlediği **günlük dinleme hedefini** (varsayılan 4 saat, 1 dk – 24 sa arası) takip eden Android uygulaması. Sayaç yalnızca ses gerçekten çalarken (`STATE_PLAYING`) ilerler; bağlantı koparsa durur. Geliştirmeye açık bir proje. Kullanıcı Türkçe iletişim kurar; arayüz Türkçe.

## Teknoloji
- Java 11, Android (compileSdk/targetSdk 35, minSdk 24), AGP 8.10.1, Gradle Kotlin DSL, tüm sürümler `gradle/libs.versions.toml`'da
- Media3 ExoPlayer 1.2.1 + `media3-exoplayer-hls` (CBC HLS için), Room 2.6.1 (annotationProcessor), RecyclerView, CardView, Material
- Takvim kütüphanesi yok (Tur 2'de material-calendarview, jitpack, Jetifier, ThreeTen kaldırıldı); ızgara kendi kodumuz. `java.time` KULLANILMAZ (minSdk 24) → `GregorianCalendar`.
- Room şeması `app/schemas/com.example.radio_timer.AppDatabase/1.json`'a dışa aktarılır (`exportSchema=true`).
- Paket: `com.example.radio_timer`

## Dosya haritası (`app/src/main/java/com/example/radio_timer/`)
| Dosya | Rol | Sahip |
|---|---|---|
| `MainActivity` | Ana ekran: üst kart (`nowPlayingCard`: istasyon, durum metni, süre, progress, yüzde, hedef, uyku ikonu + kalan süre), istasyon listesi, tarih+seri kartı, bildirimler kapalıysa uyarı satırı. Bildirim izni yalnız bir kez sorulur. onResume'da `ACTION_REQUEST_STATE` ister; eski `date_key`'li yayınları yok sayar; gece yarısı Handler ile tarih kartını yeniler. | UI |
| `SettingsActivity` | Hedef girişi (virgül/nokta) + ChipGroup kısayolları (chip anında kaydeder; programatik işaretleme kaydetmez). Kayıtta `StatsHelper.recomputeGoalFlag` çağırır. | UI |
| `CalendarActivity` | Kendi ay ızgarası (RecyclerView, `GridLayoutManager(7)`), Türkçe ay/gün adları, Pazartesi başlangıç. Yeşil = hedef tuttu, kırmızı = kayıtlı geçmiş gün tutmadı, bugün halka, gelecek soluk. Ok tuşlarıyla ay değişimi (seçim sıfırlanır), güne dokununca detay; ay/seçim döndürmede korunur. | UI |
| `MonthGridAdapter` | Takvim hücreleri (`item_calendar_day.xml`); null hücre boş/tıklanamaz. | UI |
| `StationAdapter` | İstasyon satırları: aksan etiketi, çalma ikonu, bağlanıyor/hata göstergesi (`interrupted` = çalıyor gibi). | UI |
| `RadioService` | Foreground service, durum makinesi (`PlaybackDecisions` ile), ExoPlayer (`ensurePlayer()` ile lazy), audio focus/noisy/wake, saniyelik Handler tick (`tickRunning` bayrağıyla, yalnız aktif durumlarda), 15 sn'de bir + `onTaskRemoved`/`onDestroy`'da kayıt, gün değişimi, uyku zamanlayıcı (son 30 sn ses kısma, bitince `handleStop`), bildirimler (oynatma ID 1, kutlama ID 2). | Backend |
| `PlaybackDecisions` | Saf: `resolveState` (8 öncelikli durum kuralı), `decideToggle` (PAUSE/RESUME/REPREPARE, `STALE_PAUSE_MS`=30 sn), `counterShouldRun`, `isSuppressed`. | Backend |
| `CalendarMath` | Saf: `monthCells` (Pazartesi başlangıçlı, 7'nin katı), `daysInMonth`, `addMonths`, `currentYearMonth`. | Backend |
| `StationRepository` | Sabit 6 istasyon listesi (tek kaynak), `getDefault()`, `findByUrl()`. | Backend |
| `Station` | Değişmez model: ad, URL, aksan (`ACCENT_*`). | Backend |
| `AppDatabase` / `ListeningDao` / `ListeningLog` | Room v1, `listening_logs(date PK "yyyy-MM-dd", durationMillis, isGoalReached)`. DAO: `getLogByDate`, `getLogsForMonth`, `getTotalTimeForMonth` (COALESCE), `getReachedDates`, `markGoalReachedIfAtLeast`. | Backend |
| `PreferenceHelper` | Hedef (MIN/MAX sınırlı), son istasyon, son kutlama tarihi, bildirim izni soruldu mu. | Backend |
| `StatsHelper` | Yüzde (0–100), seri hesabı, `recomputeGoalFlag`. | Backend |
| `TimeFormatter` / `DateKeys` | "sa/dk/sn" biçimleri, geri sayımlar için `roundUpToMinute`; Locale.US tarih anahtarları. | Backend |
| `AppExecutors` | Tek thread'li `diskIO()` (tüm DB işleri burada, sıra korunur). | Backend |

Testler: `app/src/test/.../DateKeysTest`, `StatsHelperTest`, `TimeFormatterTest`, `PlaybackDecisionsTest`, `CalendarMathTest` (56 test).
Kaynaklar: tüm metinler `res/values/strings.xml`'de; renkler token olarak `values/colors.xml` + `values-night/colors.xml`.

## Servis ⇄ Activity sözleşmesi (ayrıntı: `docs/tur1-kararlar.md` §2, `docs/tur2-kararlar.md` §3)
- Servis → UI broadcast: `RadioService.ACTION_STATE_UPDATE` (`setPackage` ile; UI `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)`). Extra'lar: `millis_played, daily_goal, goal_reached, date_key, is_playing, player_state, error_code, error_message, station_url, station_name, sleep_remaining_ms` — her zaman `RadioService.EXTRA_*` sabitleriyle kullan.
- `player_state`: `idle | connecting | buffering | playing | paused | interrupted | error`; `error_code`: `network | source | unknown`. `interrupted` = playWhenReady açık ama oynatma bastırılmış (kısa focus kaybı); sayaç ilerlemez.
- `is_playing` kesin olarak playWhenReady demek (connecting/buffering/playing/interrupted ⇒ true).
- `date_key` sözlük sırasıyla karşılaştırılır; UI son gösterdiğinden küçük anahtarlı yayını yok sayar.
- UI → Servis: `ACTION_PLAY_STATION` (url+name), `ACTION_TOGGLE` (startForegroundService), `ACTION_STOP`, `ACTION_SLEEP_TIMER` (`sleep_minutes` int, 0 = iptal; yalnız `startService` ve yalnız idle değilken), `ACTION_REQUEST_STATE` (yalnız `startService`, try/catch; servis boşsa startForeground çağırmadan kapanır).
- URL'siz `ACTION_PLAY_STATION`: önce startForeground, sonra oturum yoksa durur, varsa yok sayılır.
- `START_NOT_STICKY`; duraklatınca servis foreground'da kalır, "Kapat" veya uyku zamanlayıcının bitişi ile tamamen durur.

## Build / test
- Proje kökünden (Git Bash): `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && ./gradlew assembleDebug testDebugUnitTest lintDebug`
- Gerçek cihaz: Redmi `23021RAAEG` (API 35, MIUI), USB hata ayıklama. Kilitliyse ekran görüntüsü siyah gelir → kullanıcıdan açmasını iste; test sırasında `adb shell svc power stayon usb`, sonunda `false`.
- Emülatör: AVD `research` (API 34). `adb` komutlarında Git Bash için `MSYS_NO_PATHCONV=1` gerekir. Emülatör açılışta "System UI isn't responding" verebilir → "Wait".
- Media3'ün `@UnstableApi` sınıflarını kullanan metotlara `@OptIn(markerClass = UnstableApi.class)` ekle, yoksa lint hata verir.

## Ekip çalışma modeli (alt ajanlar)
1. **Proje Yöneticisi** — raporları birleştirir, KABUL/BACKLOG kararı, dosya sahipliği ve arayüz sözleşmesini sabitler.
2. **Arayüz Sorumlusu** — 4 Activity/adapter + tüm `res/`.
3. **Backend Sorumlusu** — servis, Room, yardımcılar, gradle, manifest, `app/src/test`.
4. **Kullanışlılık Önericisi** — öneri sunar; kabul edilenlerin uygulanışını doğrular.
5. **Denetçi** — hata/eksik bulur, uygulama sonrası doğrular.
Akış: (4,5 rapor) → 1 karar → (2,3 paralel uygulama) → derleme → (4,5 doğrulama) → gerekirse kısa düzeltme turu → CLAUDE.md güncelle.

## Bilinen sorunlar / Backlog (yol haritası: `docs/tur2-kararlar.md` §8)
1. **Tur 3 (özellik, şemasız):** B4/U8 haftalık özet kartı (takvim üstünde), R1 günlük "bugün henüz dinlemedin" hatırlatması (AlarmManager, BOOT_COMPLETED), hafta sonu için ayrı hedef.
2. **Tur 4 (bağımlılık):** Media3 1.2.1 → 1.9.x (compileSdk 35 ile uyumlu en yüksek), release'de R8 + keep kuralları (T11), diğer androidx güncellemeleri.
3. **Tur 5 (MediaSession):** MediaSessionService (kilit ekranı/Bluetooth, izin reddinde de kontroller), duraklatılmış servisin foreground yaşam döngüsü (T6), B2 otomatik yeniden deneme (üstel geri çekilme, NetworkCallback). MIUI'de oynatma bildirimi daraltılmış görünüyor; Duraklat/Kapat düğmeleri ancak genişletince çıkıyor → medya bildirimiyle çözülecek.
4. **Tur 6 (Room v2):** U10 kullanıcı istasyon ekleme/favoriler — gerçek `Migration(1,2)`/`@AutoMigration` + room-testing; `fallbackToDestructiveMigration` aynı turda kaldırılmalı (aksi halde şema değişikliği tüm geçmişi siler!). `1.json` hazır.
5. U11 tam tema cilası; U12 kapsamlı erişilebilirlik testi (TalkBack, 48dp, font ölçekleme).
6. Lint uyarıları (hata yok): bağımlılık sürümleri (Tur 4), `NotifyDataSetChanged` ×3, `PluralsCandidate` ×3, `MonochromeLauncherIcon`, kullanılmayan `ic_launcher_foreground`, manifest'te gereksiz label.
7. Uyku zamanlayıcının bitişi (ses kısma + durma) ve `interrupted` durumu cihazda canlı denenmedi; birim test + kod incelemesiyle doğrulandı. Android 7.x'te hiç denenmedi.
8. Avustralya/Yeni Zelanda istasyonu yok (denenen ABC/RNZ URL'leri çalışmadı, 2026-11-25).

## Değişiklik Günlüğü
- **2026-11-25 — İlk analiz.** Bu dosya oluşturuldu. Tespit: NPR URL'si 404, CBC URL'si geçersiz ve HLS desteklenmiyordu (son commit "new radio station added but not working"un nedeni).
- **2026-11-25 — Tur 1 (ekip).** 34 madde kabul edildi (karar dokümanı: `docs/tur1-kararlar.md`). Öne çıkanlar:
  - Çökmeler: startForeground gecikmesi (FGS crash), API 24-25'te NotificationChannel/registerReceiver çökmeleri giderildi.
  - Veri: gece yarısı sayaç sıfırlama, yükleme yarışı ve sırasız kayıtlar düzeltildi; tek thread'li `AppExecutors`.
  - Oynatma: durum makinesi (bağlanıyor/arabellek/hata), `onPlayerError`, HLS desteği, audio focus, kulaklık çıkınca durma, wake lock.
  - Güvenlik: paket önekli, dışa kapalı broadcast; `usesCleartextTraffic` kaldırıldı.
  - İstasyonlar (curl ile doğrulandı): BBC World Service, LBC News (UK), NPR News, WNYC, KEXP (US), CBC Radio One (CA, HLS).
  - Özellikler: hedef kısayol chip'leri, hedefe ulaşınca kutlama bildirimi, seri (streak), son istasyonu hatırlama, takvimde gün detayı ve ay değişimi, bildirimde Kapat/uygulamayı açma.
  - Arayüz: sabit metinler `strings.xml`'e, renkler token'lara + karanlık mod; "sa/dk/sn" biçimi; Ayarlar ekranında edge-to-edge insets.
  - Build: tek dependencies bloğu, version catalog, `media3-ui` kaldırıldı.
- **2026-11-25 — Tur 1b (doğrulama sonrası düzeltmeler).** Hedef değişince bugünkü `isGoalReached` yeniden hesaplanıyor (bayrak düşürülmez); sahte PAUSED yayını ve duraklatılmışken saniyelik uyanma kaldırıldı; player lazy kuruluyor; hata ipucu ve satırda "Hata"; "seri risk altında" metni; chip anında kaydediyor; 26 birim testi eklendi.
- **Doğrulama:** `assembleDebug` + 26/26 test + lint (0 hata) temiz. Emülatörde (API 34) denendi: açılış, CBC (HLS) ve LBC çalma, istasyon değiştirme, duraklatma, bildirim, takvim ekranı, hedef chip'i, uygulama kapatılıp açılınca durum geri yükleme — çökme yok. Commit `1dee609`.
- **2026-11-25/26 — Tur 2 (kararlılık + sadeleştirme + uyku zamanlayıcı).** 13 kalem kabul (`docs/tur2-kararlar.md`):
  - Uyku zamanlayıcı (15/30/45/60 dk, son 30 sn ses kısma, bildirimde alt metin).
  - Kendi takvim ızgarası (Türkçe, Pazartesi başlangıç); material-calendarview, jitpack, Jetifier, ThreeTen ve `RadioTimerApp` kaldırıldı.
  - `PlaybackDecisions` + yeni `interrupted` durumu: kısa focus kaybında gereksiz yeniden bağlanma (eski Y2) ve yanlış "duraklatıldı" görünümü giderildi.
  - Veri: kayıt aralığı 15 sn, uygulama kaydırılıp kapatılınca da kayıt; tick yeniden kurulumu bayrağa bağlandı; eski `date_key` yayınları yok sayılıyor.
  - Hedef düşürülünce de kutlama; URL'siz `PLAY_STATION` kuralı; chip'in döndürmede tekrar kaydetmesi engellendi.
  - Bildirim izni yalnız bir kez soruluyor, kapalıysa ana ekranda uyarı satırı; istasyon listesi sıkılaştırıldı.
  - Room `exportSchema=true` + `1.json` (şema v1, değişmedi).
  - Doğrulama düzeltmeleri (2026-11-26): ay değişince önceki ayın gün detayı ekranda kalıyordu → seçim sıfırlanıyor; kartta uyku süresi aşağı yuvarlanıp bildirimle çelişiyordu ("14 dk"/"15 dk") → ortak `TimeFormatter.roundUpToMinute`.
- **Doğrulama (Tur 2):** `assembleDebug` + 56/56 test + lint (0 hata) temiz; bağımlılık ağacında `com.android.support`/threeten/calendarview yok. **İlk gerçek cihaz testi** (Redmi, API 35): açılış, BBC çalma, uyku zamanlayıcı ayarlama/iptal (kart + bildirim + toast), duraklat/devam (sayaç duruyor), takvim (Türkçe ızgara, gün detayı, ay ve yıl geçişi, döndürmede korunma), yeniden kurulumda sürenin korunması — çökme yok.
