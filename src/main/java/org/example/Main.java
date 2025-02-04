package org.example;

import java.io.*;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

    private static final String FILENAME = "numbers.txt";
    private static final Random random = new Random();
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition evenCondition = lock.newCondition();
    private static final Condition oddCondition = lock.newCondition();
    private static boolean isEvenTurn = true;

    public static void main(String[] args) {
        Thread evenThread = new Thread(new EvenNumberWriter());
        Thread oddThread = new Thread(new OddNumberWriter());
        Thread readerThread = new Thread(new FileReaderTask());

        evenThread.start();
        oddThread.start();
        readerThread.start();
    }

    /**
     * Класс EvenNumberWriter генерирует и записывает четные числа в файл.
     * В бесконечном цикле он генерирует случайные четные числа от 2 до 1000,
     * соблюдая порядок записи с нечетным потоком с помощью блокировок и условий.
     */
    static class EvenNumberWriter implements Runnable {
        @Override
        public void run() {
            while (true) {
                int evenNumber = random.nextInt(500) * 2 + 2;
                lock.lock();
                try {
                    while (!isEvenTurn) {
                        evenCondition.await();
                    }
                    writeToFile(evenNumber);

                    // Передаем очередь не четному потоку
                    isEvenTurn = false;

                    oddCondition.signal();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * Класс OddNumberWriter генерирует и записывает не четные числа в файл.
     * В бесконечном цикле он генерирует случайные не четные числа от 1 до 999,
     * соблюдая порядок записи с четным потоком с помощью блокировок и условий.
     */
    static class OddNumberWriter implements Runnable {
        @Override
        public void run() {
            while (true) {
                int oddNumber = random.nextInt(500) * 2 + 1;
                lock.lock();
                try {
                    while (isEvenTurn) {
                        oddCondition.await();
                    }
                    writeToFile(oddNumber);

                    // Передаем очередь четному потоку
                    isEvenTurn = true;

                    evenCondition.signal();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * Записывает указанное число в файл, добавляя его в конец.
     * Использует BufferedWriter для эффективной записи.
     * В случае ошибки ввода-вывода выводит сообщение об ошибке в стандартный поток ошибок.
     *
     * @param number число, которое нужно записать в файл.
     */
    private static void writeToFile(int number) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILENAME, true))) {
            writer.write(number + "\n");
        } catch (IOException e) {
            System.err.println("Ошибка записи в файл: " + e.getMessage());
        }
    }

    /**
     * Класс FileReaderTask реализует интерфейс Runnable и отвечает за
     * периодическое чтение новых строк из файла.
     * Хранит позицию последнего прочитанного символа в lastReadPosition
     * и использует RandomAccessFile для чтения с этой позиции.
     * Ошибки при чтении выводятся в стандартный поток ошибок.
     * 
     */
    static class FileReaderTask implements Runnable {

        private long lastReadPosition = 0;

        @Override
        public void run() {
            while (true) {
                readFromFile();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        /**
         * Читает новые строки из файла, начиная с позиции lastReadPosition.
         * Если файл не существует, метод завершает работу.
         * Считывает строки с помощью RandomAccessFile и выводит их в консоль.
         */
        private void readFromFile() {
            File fileEx = new File(FILENAME);
            if (!fileEx.exists()) {
                return;
            }

            try (RandomAccessFile file = new RandomAccessFile(FILENAME, "r")) {
                file.seek(lastReadPosition);
                String line;
                while ((line = file.readLine()) != null) {
                    System.out.println(line);
                }
                lastReadPosition = file.getFilePointer();
            } catch (IOException e) {
                System.err.println("Ошибка чтения из файла: " + e.getMessage());
            }
        }
    }
}

