package net.chaosjukebox.audio;

import net.chaosjukebox.ChaosJukeboxMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Дістає ПРЯМЕ посилання на аудіодорожку відео через зовнішню програму yt-dlp.
 *
 * ВАЖЛИВО: цей мод НЕ качає і не вшиває в себе сам YouTube/відео.
 * Він просто питає у yt-dlp (який має бути встановлений окремо на ПК,
 * https://github.com/yt-dlp/yt-dlp) "яка пряма URL у аудіо цього відео",
 * і потім стрімить цей звук через ffmpeg. Це той самий підхід, який
 * використовують музичні Discord-боти.
 *
 * Якщо yt-dlp / ffmpeg не встановлені або не додані у PATH - буде кинуто
 * виключення з людським поясненням, яке зловиться в StreamAudioPlayer
 * і покажеться гравцю в чаті.
 */
public final class YoutubeResolver {

    private YoutubeResolver() {}

    /**
     * @param videoUrl посилання, яке гравець вставив у GUI (youtube.com/watch?v=... або youtu.be/...)
     * @return пряме URL аудіопотоку (webm/m4a), яке можна віддати ffmpeg
     */
    public static String resolveDirectAudioUrl(String videoUrl) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "yt-dlp",
                "-f", "bestaudio",
                "--no-playlist",
                "-g", // get-url, нічого не качаємо на диск
                videoUrl
        );
        pb.redirectErrorStream(false);
        Process process = pb.start();

        String directUrl;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            directUrl = reader.readLine();
        }

        String errorOutput;
        try (BufferedReader errReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = errReader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            errorOutput = sb.toString();
        }

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("yt-dlp не відповів за 30 секунд (можливо проблема з мережею).");
        }

        if (directUrl == null || directUrl.isBlank()) {
            ChaosJukeboxMod.LOGGER.warn("yt-dlp stderr: {}", errorOutput);
            throw new IllegalStateException(
                    "Не вдалось дістати аудіо з посилання. Перевір, що yt-dlp встановлено " +
                            "(https://github.com/yt-dlp/yt-dlp) і додано в PATH, та що лінк правильний."
            );
        }

        return directUrl.trim();
    }
}
