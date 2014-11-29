package org.adrianwalker.randomgutenberg;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import static java.lang.String.format;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

public final class RandomGutenberg {

  private static final Logger LOGGER = Logger.getLogger(RandomGutenberg.class);

  private static final int GUTTENBERG_BOOK_COUNT = 47373;
  private static final String GUTTENBERG_BOOK_URL = "http://www.gutenberg.org/files/%s/%s.txt";
  private static final String SENTANCE_ENDINGS_REGEX = "(?<=\\s?\"?[.!?]\"?\\s?)";
  private static final String WORD_ENDINGS_REGEX = "\\s";
  private static final Pattern SENTENCE_ENDINGS_PATTERN = Pattern.compile(SENTANCE_ENDINGS_REGEX);
  private static final Pattern WORD_ENDINGS_PATTERN = Pattern.compile(WORD_ENDINGS_REGEX);
  private static final int MAX_TRIES = 3;
  private static final int MAX_TWEET_LENGTH = 140;
  private static final int MIN_WORD_COUNT = 3;

  public static void main(final String[] args) {

    Random randomNumberGenerator = new Random(new Random(System.currentTimeMillis()).nextLong());

    String eBookText = null;

    for (int tries = 0; tries < MAX_TRIES; tries++) {
      try {
        eBookText = getRandomEbookText(randomNumberGenerator);
        break;
      } catch (final Throwable t) {
        LOGGER.error("Error getting eBook text", t);
      }
    }

    if (null == eBookText) {
      return;
    }

    String sentence = getRandomSentence(eBookText, randomNumberGenerator);

    try {
      tweet(sentence);
    } catch (final Throwable t) {
      LOGGER.error("Error sending tweet", t);
    }
  }

  private static String getRandomSentence(final String eBookText, final Random randomNumberGenerator) {

    List<String> sentences = new ArrayList<>(Arrays.asList(SENTENCE_ENDINGS_PATTERN.split(eBookText)));

    Iterator<String> sentenceIterator = sentences.iterator();
    while (sentenceIterator.hasNext()) {

      String sentence = sentenceIterator.next();
      String[] words = WORD_ENDINGS_PATTERN.split(sentence);

      int sentenceLength = sentence.length();
      int wordCount = words.length;

      if (wordCount < MIN_WORD_COUNT || sentenceLength > MAX_TWEET_LENGTH) {
        sentenceIterator.remove();
      }
    }

    String sentence = sentences.get(randomNumberGenerator.nextInt(sentences.size()));

    return sentence;
  }

  private static String getRandomEbookText(final Random randomNumberGenerator) throws Throwable {

    int eBookId = randomNumberGenerator.nextInt(GUTTENBERG_BOOK_COUNT) + 1;
    URL url = new URL(format(GUTTENBERG_BOOK_URL, eBookId, eBookId));

    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
    StringBuilder eBookBuffer = new StringBuilder();

    String line;
    while (null != (line = reader.readLine())) {

      line = line.trim();

      if (eBookBuffer.length() > 0) {
        eBookBuffer.append(" ");
      }

      eBookBuffer.append(line);
    }

    String eBookText = eBookBuffer.toString();

    return eBookText;
  }

  private static Status tweet(final String sentence) throws Throwable {

    String message = sentence;

    Twitter twitter = TwitterFactory.getSingleton();
    Status status = twitter.updateStatus(message);

    return status;
  }
}
