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
    private ArrayList<String> list = new ArrayList<>();

    private UserInfo me;
    private UserInfo she;
    private int questionNumber; //номер вопроса

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

/* **** Режим PROFILE *********************************************************************************************** */
        if (message.equals("/profile")) {
            currentMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");

            //обнуляем инфо о человеке
            me = new UserInfo();
            questionNumber = 1; //задаём 1ый вопрос
            sendTextMessage("Сколько Вам лет?");
            return;
        }

        if (currentMode == DialogMode.PROFILE && !isMessageCommand()) {
            //ответы на вопросы
            switch (questionNumber) {
                case 1:
                    me.age = message;
                    questionNumber = 2; //2ой вопрос
                    sendTextMessage("Кем Вы работаете?");
                    return;
                case 2:
                    me.occupation = message;
                    questionNumber = 3; //3ий вопрос
                    sendTextMessage("У Вас есть хобби?");
                    return;
                case 3:
                    me.hobby = message;
                    questionNumber = 4; //4ый вопрос
                    sendTextMessage("Что Вам НЕ нравится в людях?");
                    return;
                case 4:
                    me.annoys = message;
                    questionNumber = 5; //5ый вопрос
                    sendTextMessage("Цель знакомства?");
                    return;
                case 5:
                    me.goals = message;
                    //после ответа на последний вопрос отправляем все данныве в ChatGPT
                    String aboutMyself = me.toString();
                    String prompt = loadPrompt("profile");
                    Message msg = sendTextMessage("Подождите пару секунд - ChatGPT \uD83E\uDDE0 думает...");
                    String answer = chatGPT.sendMessage(prompt, aboutMyself);
                    updateTextMessage(msg, answer); //меняем сообщение (что, на что)
                    return;
            }

            return;
        }

/* **** Режим OPENER ************************************************************************************************ */
        if (message.equals("/opener")) {
            currentMode = DialogMode.OPENER;
            sendPhotoMessage("opener");

            she = new UserInfo();
            questionNumber = 1; //задаём 1ый вопрос
            sendTextMessage("Имя девушки?");
            return;
        }

        if (currentMode == DialogMode.OPENER && !isMessageCommand()) {
            switch (questionNumber) {
                case 1:
                    she.name = message;
                    questionNumber = 2; //2ой вопрос
                    sendTextMessage("Сколько Ей лет?");
                    return;
                case 2:
                    she.age = message;
                    questionNumber = 3; //3ий вопрос
                    sendTextMessage("Есть ли у Неё хобби?");
                    return;
                case 3:
                    she.hobby = message;
                    questionNumber = 4; //4ый вопрос
                    sendTextMessage("Кем Она работает?");
                    return;
                case 4:
                    she.occupation = message;
                    questionNumber = 5; //5ый вопрос
                    sendTextMessage("Цель знакомства?");
                    return;
                case 5:
                    she.goals = message;
                    //после ответа на последний вопрос отправляем все данныве в ChatGPT
                    Message msg = sendTextMessage("Подождите пару секунд - ChatGPT \uD83E\uDDE0 думает...");
                    String aboutFriend = she.toString();
                    String prompt = loadPrompt("opener");
                    String answer = chatGPT.sendMessage(prompt, aboutFriend);
                    updateTextMessage(msg, answer); //меняем сообщение (что, на что)
                    return;
            }
            return;
        }

/* **** Режим MESSAGE *********************************************************************************************** */
        if (message.equals("/message")) {
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage("Пришлите в чат вашу переписку.",
                    "Следующее сообщение", "message_next",
                    "Пригласить на свидание", "message_date");
            return;
        }

        if (currentMode == DialogMode.MESSAGE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message_")) { //начинается с… “message_”
                String prompt = loadPrompt(query);
                String userChatHistory = String.join("\n\n", list);

                Message msg = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
                String answer = chatGPT.sendMessage(prompt, userChatHistory);
                updateTextMessage(msg, answer); //меняем сообщение (что, на что)
                return;
            }
            //в противном случае все сообщения пользователя складируем
            list.add(message);
            return;
        }

/* **** Режим DATE ************************************************************************************************* */
        if (message.equals("/date")) {
            currentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            sendTextMessage("Выберите девушку, которую хотите пригласить на свидание.");

            String text = loadMessage("date"); //вместо ручного ввода
            sendTextButtonsMessage(text,
                    "Ариана Гранде", "date_grande",
                    "Марго Робби", "date_robbie",
                    "Зендея", "date_zendaya",
                    "Райян Гослинг", "date_gosling",
                    "Том Харди", "date_hardy");
            return;
        }

        if (currentMode == DialogMode.DATE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("date_")) { //начинается с… “date_”
                sendPhotoMessage(query);
                sendTextMessage("Отличный выбор!\nТвоя задача пригласить девушку на свидание ❤\uFE0F за 5 сообщений.");

                //chatGPT.setPrompt("Диалог с девушкой");
                String prompt = loadPrompt(query);
                chatGPT.setPrompt(prompt);
                return;
            }

            Message msg = sendTextMessage("Подождите, девушка набирает текст...");
            String answer = chatGPT.addMessage(message);
            updateTextMessage(msg, answer);
            return;
        }

/* **** Режим GPT ************************************************************************************************** */
        if (message.equals("/gpt")) {
            currentMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            String text = loadMessage("gpt"); //вместо ручного ввода строки: "Напишите ваше сообщение для *ChatGPT*"
            sendTextMessage(text);
            return;
        }

        if (currentMode == DialogMode.GPT && !isMessageCommand()) {
            String prompt = loadPrompt("gpt");
            //включен режим GPT, т.е. сюда попадают сообщение для ChatGPT. Перенаправим их в ChatGPT:
            Message msg = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
            String answer = chatGPT.sendMessage(prompt, message);
            //а ответ от ChatGPT передаем обратно:
            updateTextMessage(msg, answer);
            return;
        }
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}



