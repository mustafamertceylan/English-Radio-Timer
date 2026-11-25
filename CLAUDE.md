# Radio_Timer — Proje Hatırlatıcısı

## Amaç
İngilizce öğrenmek için İngilizce radyo istasyonlarını dinleten ve kullanıcının belirlediği **günlük dinleme hedefini** (varsayılan 4 saat, 1 dk – 24 sa arası) takip eden Android uygulaması. Sayaç yalnızca ses gerçekten çalarken (`STATE_PLAYING`) ilerler; bağlantı koparsa durur. Geliştirmeye açık bir proje. Kullanıcı Türkçe iletişim kurar; arayüz Türkçe.

## Teknoloji
- Java 11, Android (compileSdk/targetSdk 35, minSdk 24), AGP 8.10.1, Gradle Kotlin DSL, tüm sürümler `gradle/libs.versions.toml`'da
- Media3 ExoPlayer 1.2.1 + `media3-exoplayer-hls` (CBC HLS için), Room 2.6.1 (annotationProcessor), RecyclerView, CardView, Material
- `com.github.prolificinteractive:material-calendarview:2.0.1` (jitpack, eski support lib'e bağlı → Jetifier) + `threetenabp` (`RadioTimerApp`'te init edilir)
- Paket: `com.example.radio_timer`

## Dosya haritası (`app/src/main/java/com/example/radio_timer/`)
| Dosya | Rol | Sahip |
|---|---|---|
| `MainActivity` | Ana ekran: üst kart (`nowPlayingCard`: istasyon, durum metni, süre, progress, yüzde, hedef), istasyon listesi, tarih+seri kartı. onResume'da `ACTION_REQUEST_STATE` ister; gece yarısı Handler ile tarih kartını yeniler. | UI |
| `SettingsActivity` | Hedef girişi (virgül/nokta) + ChipGroup kısayolları (chip anında kaydeder). Kayıtta `StatsHelper.recomputeGoalFlag` çağırır. | UI |
| `CalendarActivity` | Ay bazlı: `isGoalReached`'e göre yeşil/kırmızı (bugün boyanmaz), ay değişince başlık/toplam, güne dokununca detay. | UI |
| `StationAdapter` | İstasyon satırları: aksan etiketi, çalma ikonu, bağlanıyor/hata göstergesi. | UI |
| `RadioService` | Foreground service, durum makinesi, ExoPlayer (`ensurePlayer()` ile lazy), audio focus/noisy/wake, saniyelik Handler tick (yalnız aktif durumlarda), 60 sn'de bir kayıt, gün değişimi, bildirimler (oynatma ID 1, kutlama ID 2). | Backend |
| `StationRepository` | Sabit 6 istasyon listesi (tek kaynak), `getDefault()`, `findByUrl()`. | Backend |
| `Station` | Değişmez model: ad, URL, aksan (`ACCENT_*`). | Backend |
| `AppDatabase` / `ListeningDao` / `ListeningLog` | Room v1, `listening_logs(date PK "yyyy-MM-dd", durationMillis, isGoalReached)`. DAO: `getLogByDate`, `getLogsForMonth`, `getTotalTimeForMonth` (COALESCE), `getReachedDates`, `markGoalReachedIfAtLeast`. | Backend |
| `PreferenceHelper` | Hedef (MIN/MAX sınırlı), son istasyon, son kutlama tarihi. | Backend |
| `StatsHelper` | Yüzde (0–100), seri hesabı, `recomputeGoalFlag`. | Backend |
| `TimeFormatter` / `DateKeys` | "sa/dk/sn" biçimleri; Locale.US tarih anahtarları. | Backend |
| `AppExecutors` | Tek thread'li `diskIO()` (tüm DB işleri burada, sıra korunur). | Backend |
| `RadioTimerApp` | Application: ThreeTen init. | Backend |

Testler: `app/src/test/.../DateKeysTest`, `StatsHelperTest`, `TimeFormatterTest` (26 test).
Kaynaklar: tüm metinler `res/values/strings.xml`'de; renkler token olarak `values/colors.xml` + `values-night/colors.xml`.

## Servis ⇄ Activity sözleşmesi (ayrıntı: `docs/tur1-kararlar.md` §2)
- Servis → UI broadcast: `RadioService.ACTION_STATE_UPDATE` (`setPackage` ile; UI `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)`). Extra'lar: `millis_played, daily_goal, goal_reached, date_key, is_playing, player_state, error_code, error_message, station_url, station_name` — her zaman `RadioService.EXTRA_*` sabitleriyle kullan.
- `player_state`: `idle | connecting | buffering | playing | paused | error`; `error_code`: `network | source | unknown`.
- UI → Servis: `ACTION_PLAY_STATION` (url+name), `ACTION_TOGGLE` (startForegroundService), `ACTION_STOP`, `ACTION_REQUEST_STATE` (yalnız `startService`, try/catch; servis boşsa startForeground çağırmadan kapanır).
- `START_NOT_STICKY`; duraklatınca servis foreground'da kalır, "Kapat" ile tamamen durur.

## Build / test
- Proje kökünden (Git Bash): `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && ./gradlew assembleDebug testDebugUnitTest lintDebug`
- Emülatör: AVD `research` (API 34). `adb` komutlarında Git Bash için `MSYS_NO_PATHCONV=1` gerekir. Emülatör açılışta "System UI isn't responding" verebilir → "Wait".
- Media3'ün `@UnstableApi` sınıflarını kullanan metotlara `@OptIn(markerClass = UnstableApi.class)` ekle, yoksa lint hata verir.

## Ekip çalışma modeli (alt ajanlar)
1. **Proje Yöneticisi** — raporları birleştirir, KABUL/BACKLOG kararı, dosya sahipliği ve arayüz sözleşmesini sabitler.
2. **Arayüz Sorumlusu** — 4 Activity/adapter + tüm `res/`.
3. **Backend Sorumlusu** — servis, Room, yardımcılar, gradle, manifest, `app/src/test`.
4. **Kullanışlılık Önericisi** — öneri sunar; kabul edilenlerin uygulanışını doğrular.
5. **Denetçi** — hata/eksik bulur, uygulama sonrası doğrular.
Akış: (4,5 rapor) → 1 karar → (2,3 paralel uygulama) → derleme → (4,5 doğrulama) → gerekirse kısa düzeltme turu → CLAUDE.md güncelle.

## Bilinen sorunlar / Backlog
1. U8 Haftalık istatistik/grafik ekranı.
2. U9 Uyku zamanlayıcı (`ACTION_SLEEP_TIMER`).
3. U10 Kullanıcı istasyon ekleme/favoriler — Room v2 + gerçek `Migration(1,2)`; aynı turda `fallbackToDestructiveMigration` kaldırılmalı, `exportSchema=true` (aksi halde şema değişikliği tüm geçmişi siler!).
4. U11 tam karanlık tema cilası (takvim widget'ı cihaz dilinde "November/Sun" gösteriyor; başlık Türkçe).
5. U12 kapsamlı erişilebilirlik testi (TalkBack, 48dp, font ölçekleme).
6. MediaSessionService geçişi (kilit ekranı / Bluetooth kontrolleri).
7. Hata sonrası otomatik yeniden deneme (üstel geri çekilme).
8. Media3 1.2.1 → güncel sürüm ve diğer bağımlılık yükseltmeleri.
9. Y2: kısa süreli audio focus kaybından sonra "Devam et" gereksiz yeniden bağlanmaya yol açabiliyor.
10. Avustralya/Yeni Zelanda istasyonu yok (denenen ABC/RNZ URL'leri çalışmadı, 2026-11-25).

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
- **Doğrulama:** `assembleDebug` + 26/26 test + lint (0 hata) temiz. Emülatörde (API 34) denendi: açılış, CBC (HLS) ve LBC çalma, istasyon değiştirme, duraklatma, bildirim, takvim ekranı, hedef chip'i, uygulama kapatılıp açılınca durum geri yükleme — çökme yok. Gerçek cihazda ve Android 7.x'te henüz denenmedi. Commit yapılmadı.
