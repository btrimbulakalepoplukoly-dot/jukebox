package net.chaosjukebox.audio;

import net.chaosjukebox.ChaosJukeboxMod;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Один екземпляр = одне відтворення на одному блоці.
 *
 * Конвеєр: yt-dlp (URL відео -> пряме URL аудіо)
 *        -> ffmpeg (пряме URL -> сирий PCM 48kHz stereo 16-bit, прямо в stdout)
 *        -> Java Sound (SourceDataLine) -> колонки гравця
 *
 * Гучність можна змінювати "на льоту" через setVolume(0..1) - саме це
 * викликає ActiveAudioManager кожен тік, рахуючи дистанцію до блока.
 *
 * Потребує встановлених в PATH: yt-dlp та ffmpeg.
 */
public class StreamAudioPlayer {

    private static final AudioFormat FORMAT = new AudioFormat(
            48000.0f, // sample rate
            16,        // bits per sample
            2,         // stereo
            true,      // signed
            false      // little endian (відповідає ffmpeg "-f s16le")
    );

    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private volatile float volume = 1.0f;
    private Process ffmpegProcess;
    private SourceDataLine line;
    private Thread workerThread;

    /**
     * Запускає відтворення в окремому потоці (мережеві виклики не повинні
     * блокувати клієнтський тік).
     *
     * @param videoUrl       посилання, яке ввів гравець
     * @param startOffsetSec з якої секунди почати (для тих, хто доєднався пізніше)
     * @param onError        викликається з повідомленням, якщо щось пішло не так (наприклад показати в чаті)
     */
    public void start(String videoUrl, double startOffsetSec, Consumer<String> onError) {
        workerThread = new Thread(() -> runPipeline(videoUrl, startOffsetSec, onError), "ChaosJukebox-Audio");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private void runPipeline(String videoUrl, double startOffsetSec, Consumer<String> onError) {
        try {
            String directAudioUrl = YoutubeResolver.resolveDirectAudioUrl(videoUrl);
            if (stopped.get()) return;

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-loglevel", "error",
                    "-ss", String.valueOf(Math.max(0, startOffsetSec)),
                    "-i", directAudioUrl,
                    "-f", "s16le",
                    "-ar", "48000",
                    "-ac", "2",
                    "-"
            );
            pb.redirectErrorStream(false);
            ffmpegProcess = pb.start();

            line = AudioSystem.getSourceDataLine(FORMAT);
            line.open(FORMAT);
            line.start();

            byte[] buffer = new byte[4096];
            InputStream audioStream = ffmpegProcess.getInputStream();
            int bytesRead;
            while (!stopped.get() && (bytesRead = audioStream.read(buffer)) != -1) {
                applyCurrentVolume();
                line.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            ChaosJukeboxMod.LOGGER.error("Помилка відтворення Chaos Jukebox", e);
            if (onError != null) {
                onError.accept(e.getMessage() != null ? e.getMessage() : "Невідома помилка відтворення.");
            }
        } finally {
            cleanup();
        }
    }

    private void applyCurrentVolume() {
        if (line == null) return;
        try {
            FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float v = Math.max(0.0001f, Math.min(1.0f, volume));
            float dB = (float) (Math.log10(v) * 20.0);
            dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
            gainControl.setValue(dB);
        } catch (IllegalArgumentException ignored) {
            // деякі звукові системи не підтримують MASTER_GAIN - просто пропускаємо
        }
    }

    /** Викликається з тіку клієнта: 0.0 = тиша (далі 30 блоків), 1.0 = повна гучність (стоїш біля блока). */
    public void setVolume(float volume) {
        this.volume = volume;
    }

    public void stop() {
        stopped.set(true);
        cleanup();
    }

    private void cleanup() {
        if (ffmpegProcess != null) {
            ffmpegProcess.destroyForcibly();
        }
        if (line != null) {
            try {
                line.stop();
                line.close();
            } catch (Exception ignored) {}
        }
    }

    public boolean isStopped() {
        return stopped.get();
    }
}
