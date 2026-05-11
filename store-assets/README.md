# Play Store görselleri

Bu klasörde Play Console ana mağaza kaydı için hazırlanmış görseller bulunur.

## Dosyalar

- `source/video-bolucu-by-enes-source.png`: uygulama ikonunun kaynak görseli
- `icon/video-bolucu-by-enes-512.png`: kaynak görselden kırpılıp ölçeklenmiş 512x512 yüksek çözünürlüklü uygulama ikonu
- `feature-graphic/video-bolucu-feature-1024x500.png`: 1024x500 feature graphic
- `screenshots/phone/phone-01-home.png`: ana ekran
- `screenshots/phone/phone-02-custom-duration.png`: özel süre seçimi
- `screenshots/phone/phone-03-processing.png`: bölme işlemi
- `screenshots/phone/phone-04-results.png`: kaydedilen parçalar ve paylaşma

## Yeniden üretme

```powershell
javac -encoding UTF-8 tools\GenerateStoreAssets.java
java -cp tools GenerateStoreAssets
```

Bu komut aynı zamanda Android launcher ikonlarını da `app/src/main/res/mipmap-*` klasörlerine üretir.

## Play Console sırası

1. `phone-01-home.png`
2. `phone-02-custom-duration.png`
3. `phone-03-processing.png`
4. `phone-04-results.png`

Alt metin önerileri:

- Ana ekranda video seçme ve parça süresi seçenekleri gösteriliyor.
- Özel süre seçimiyle video parçalama ayarı gösteriliyor.
- Video parçalama ilerleme durumu gösteriliyor.
- Kaydedilen video parçaları ve paylaşma seçenekleri gösteriliyor.
