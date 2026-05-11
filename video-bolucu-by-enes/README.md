# Video Bölücü by Enes

Android için yerel video bölme uygulaması.

Uygulama Android sistem dosya seçicisiyle video alır, videoyu 30/60/90 saniye veya kullanıcının belirlediği özel süreye göre parçalara ayırır, çıktıları `Movies/VideoBolucu` klasörüne kaydeder ve parçaları Android paylaşım ekranıyla gönderebilir.

## Özellikler

- 30, 60 ve 90 saniyelik hazır süre seçenekleri
- Özel süre belirleme
- `MediaExtractor` ve `MediaMuxer` ile yeniden encode etmeden MP4 parça üretimi
- Android 10+ için MediaStore ile izin istemeden kaydetme
- Android 8-9 için `WRITE_EXTERNAL_STORAGE` izni
- Tek parçayı veya tüm parçaları paylaşma
- İnternet izni yok
- Reklam, analitik ve hesap sistemi yok

## Teknik Not

Bu uygulama videoyu yeniden encode etmez. Bu yüzden kesimler video keyframe noktalarına göre başlayabilir; parça başlangıçları hedef saniyeden çok az önce olabilir. Frame düzeyinde kesim gerektiğinde FFmpeg veya Media3 Transformer gibi yeniden encode eden bir iş akışı gerekir.

## Paket Bilgileri

- Paket adı: `com.eneserdogan.videobolucu`
- Uygulama adı: `Video Bölücü by Enes`
- Min SDK: 26
- Target SDK: 35
- Version code: 1
- Version name: `1.0.0`

## Derleme

Projeyi Android Studio ile açıp Gradle sync çalıştırın.

Debug APK:

```powershell
.\gradlew.bat assembleDebug
```

Release AAB:

```powershell
.\gradlew.bat bundleRelease
```

Release build için kendi upload keystore dosyanızı kullanmanız gerekir. Gerçek keystore, parola dosyaları, AAB/APK çıktıları ve yerel SDK ayarları repoya eklenmemelidir.

## GitHub Pages

`docs/` klasörü Play Console için kullanılabilecek gizlilik politikası sayfasını içerir.

GitHub Pages için önerilen ayar:

- Source: `Deploy from a branch`
- Branch: `main`
- Folder: `/docs`

Yayınlandıktan sonra gizlilik politikası şu formatta açılır:

```text
https://KULLANICI_ADI.github.io/video-bolucu-by-enes/privacy.html
```

## Play Store Görselleri

Play Store görselleri `store-assets/` klasörü altındadır:

- 512x512 uygulama ikonu
- 1024x500 feature graphic
- Telefon ekran görüntüleri

Görselleri yeniden üretmek için:

```powershell
javac -encoding UTF-8 tools\GenerateStoreAssets.java
java -cp tools GenerateStoreAssets
```

## Katkı

Katkılar pull request üzerinden değerlendirilir. Projeye dahil edilecek değişiklikler maintainer onayından geçmelidir. Ayrıntılar için `CONTRIBUTING.md` dosyasına bakın.

## Lisans

Bu proje MIT lisansı ile yayınlanır. Ayrıntılar için `LICENSE` dosyasına bakın.
