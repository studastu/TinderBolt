package com.javarush.telegram;

import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "JRMaraphoneBot"; //TODO: добавь имя бота в кавычках. Ссылка: t.me/JRMaraphoneBot
    public static final String TELEGRAM_BOT_TOKEN = "7379081215:AAHmmU9X212bHtJKU-mDn2ZjPnGe0119PoA"; //TODO: добавь токен бота в кавычках
    public static final String OPEN_AI_TOKEN = "gpt:6MZuruLWYMt7BFAYy33hJFkblB3TrOQSkF7WUgsEFs26dToB"; //TODO: добавь токен ChatGPT в кавычках

    private ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentMode = null;

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        String message = getMessageText(); //узнать, что написал пользователь боту

        if (message.equals("/start")) {
            currentMode = DialogMode.MAIN;
            sendPhotoMessage("main"); //отправляем картинку из ресурсов/images (просто имя картинки, он найдет)
            String text = loadMessage("main"); //отправляем текст из ресурсов/messages
            sendTextMessage(text);

            //добавление кнопки "Меню" в телеграмме
            showMainMenu("Главное меню бота", "/start",
                    "Генерация Tinder-профиля \uD83D\uDE0E", "/profile",
                    "Сообщение для знакомства \uD83E\uDD70", "/opener",
                    "Переписка от вашего имени \uD83D\uDE08", "/message",
                    "Переписка со звездами \uD83D\uDD25", "/date",
                    "Задать вопрос чату GPT \uD83E\uDDE0", "/gpt"
                    );

            return;
        }

        if (message.equals("/profile")) {
            currentMode = DialogMode.PROFILE;
            return;
        }

        if (message.equals("/opener")) {
            currentMode = DialogMode.OPENER;
            return;
        }

        if (message.equals("/message")) {
            currentMode = DialogMode.MESSAGE;
            return;
        }

        if (message.equals("/date")) {
            currentMode = DialogMode.DATE;
            return;
        }

        if (message.equals("/gpt")) {
            currentMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            String text = loadMessage("gpt"); //вместо ручного ввода строки: "Напишите ваше сообщение для *ChatGPT*"
            sendTextMessage(text);
            return;
        }

        if (currentMode == DialogMode.GPT) {
            String prompt = loadPrompt("gpt");
            //включен режим GPT, т.е. сюда попадают сообщение для ChatGPT. Перенаправим их в ChatGPT:
            String answer = chatGPT.sendMessage(prompt, message);
            //а ответ от ChatGPT передаем обратно:
            sendTextMessage(answer);
            return;
        }


        // всякая всячина

        sendTextMessage("Ищешь секретные команды? Лови! /cool - значит тебе зашёл наш чат-бот и ты хочешь быть в теме!");

        if (message.equals("/cool")) {
            sendPhotoMessage("avatar_main");
            sendTextMessage("Согласен! Это самый крутой чат-бот в телеге!");
            sendTextMessage("Хочешь быть с нами? Пиши email - пришлём секретную ссылку.");
            return;
        }

        if (message.contains("@")) {
            sendTextMessage("Шутишь что-ли?!");
            sendTextMessage("Email: _" + message + "_ в списке запрещенных в ФБР, ЦРУ, АМБ и прочих спецслужб по всему миру!"); //_курсивом_ или *жирным*
            sendTextMessage("Ты определенно нужен нам!");

            //отправить сообщение с кнопками. пользователь нажимает на кнопку и в наш метод приходит её уникальное имя, так мы понимаем, что он нажал.
            //но имя приходит не в виде текста, т.е. getMessageText() выдаст пустоту. А как понять, что нажал юзер? Разберем на след уроке.
            sendTextButtonsMessage("Выберите режим работы:", "Морфеус", "btnGuru", "Нео", "btnMan", "Тринити", "btnWoman");
            return;
        }

        sendTextMessage("Бла-бла-бла...");
        sendTextMessage("Извини, но нам сейчас не до пустой болтовни.");
        sendTextMessage("Слишком заняты марафоном!");

    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
