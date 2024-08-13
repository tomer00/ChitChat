package com.tomer.chitchat.utils

object EmojisHashingUtils {

    val emojiList: List<String>

    //region EMOJIS


    val jHash: Map<String, String> = hashMapOf<String, String>().apply {
        put("%F0%9F%91%BD", "alien.json")
        put("%F0%9F%91%BF", "angry-face-with-horns.json")
        put("%F0%9F%98%A1", "angry.json")
        put("%F0%9F%A6%8B", "butterfly.json")
        put("%F0%9F%A4%99", "call-me-hand.json")
        put("%F0%9F%91%8F", "clapping-hands.json")
        put("%F0%9F%A4%A1", "clown-face.json")
        put("%F0%9F%8C%B9", "rose.json")
        put("%F0%9F%8C%83", "good-night.json")
        put("%F0%9F%8C%84", "good-morning.json")
        put("%F0%9F%8C%9E", "good-morning.json")
        put("%F0%9F%8C%BC", "flower-petals.json")
        put("%F0%9F%8C%B8", "sunflower.json")
        put("%F0%9F%A4%AC", "cussing.json")
        put("%F0%9F%A4%94", "thinking.json")
        put("%F0%9F%8C%9F", "star.json")
        put("%F0%9F%94%86", "sun.json")
        put("%F0%9F%92%97", "growing-heart.json")
        put("%F0%9F%98%98", "kiss.json")
        put("%F0%9F%98%82", "laughing-tears.json")
        put("%F0%9F%98%84", "laughing.json")
        put("%F0%9F%99%8F", "namaste.json")
        put("%F0%9F%A5%B2", "sad-tear.json")
        put("%F0%9F%91%8D", "thumbs-up.json")
        put("%F0%9F%A4%AE", "vomiting.json")
        put("%E2%9D%A4%EF%B8%8F", "heart.json")
        put("%E2%98%80%EF%B8%8F", "sun.json")
        put("%F0%9F%87%AE%F0%9F%87%B3", "indian_flag.json")
        put("%E2%98%94", "rain.json")
        put("%3F%3F", "ques.json")
        put("GN", "good-night.json")
        put("GM", "good-morning.json")
        put("gn", "good-night.json")
        put("gm", "good-morning.json")

        put("%F0%9F%92%A3", "bomb.json")
    }

    val teleHash: Map<String, String> = hashMapOf<String, String>().apply {

        //people
        put("%F0%9F%95%BA", "People/Man%20Dancing.webp")
        put("%F0%9F%92%83", "People/Woman%20Dancing.webp")
        put("%F0%9F%91%A8", "People/Oncoming%20Fist.webp")
        put("%F0%9F%91%B6", "People/Baby.webp")
        put("%F0%9F%91%87", "People/Backhand%20Index%20Pointing%20Down.webp")
        put("%F0%9F%91%88", "People/Backhand%20Index%20Pointing%20Left.webp")
        put("%F0%9F%91%89", "People/Backhand%20Index%20Pointing%20Right.webp")
        put("%F0%9F%91%86", "People/Backhand%20Index%20Pointing%20Up.webp")
        put("%E2%98%9D%EF%B8%8F", "People/Backhand%20Index%20Pointing%20Up.webp")
        put("%F0%9F%AB%B0", "People/Hand%20With%20Index%20Finger%20And%20Thumb%20Crossed.webp")
        put("%F0%9F%96%90", "People/Hand%20With%20Fingers%20Splayed.webp")
        put("%F0%9F%AB%B5", "People/Index%20Pointing%20At%20The%20Viewer.webp")
        put("%F0%9F%A4%9F", "People/Love%20You%20Gesture.webp")
        put("%F0%9F%91%8C", "People/Ok%20Hand.webp")
        put("%F0%9F%AB%B3", "People/Palm%20Down%20Hand.webp")
        put("%F0%9F%AB%B4", "People/Palm%20Up%20Hand.webp")
        put("%F0%9F%A4%8C", "People/Pinched%20Fingers.webp")
        put("%F0%9F%A4%8F", "People/Pinching%20Hand.webp")
        put("%F0%9F%96%96", "People/Vulcan%20Salute.webp")

        put("%F0%9F%A4%A6", "People/Person%20Facepalming.webp")
        put("%F0%9F%A4%B7", "People/Person%20Shrugging.webp")
        put("%F0%9F%A4%B7%E2%80%8D%E2%99%82%EF%B8%8F", "People/Man%20Shrugging.webp")
        put("%F0%9F%A4%A6%E2%80%8D%E2%99%82%EF%B8%8F", "People/Man%20Facepalming.webp")
        put("%F0%9F%A4%B7%E2%80%8D%E2%99%80%EF%B8%8F", "People/Woman%20Shrugging.webp")
        put("%F0%9F%A4%A6%E2%80%8D%E2%99%80%EF%B8%8F", "People/Woman%20Facepalming.webp")


        //Animals
        put("%F0%9F%90%B6", "Animals%20and%20Nature/Dog%20Face.webp")
        put("%F0%9F%90%BB", "Animals%20and%20Nature/Bear.webp")
        put("%F0%9F%90%84", "Animals%20and%20Nature/Cow.webp")
        put("%F0%9F%A6%81", "Animals%20and%20Nature/Lion.webp")
        put("%E2%98%83%EF%B8%8F", "Animals%20and%20Nature/Snowman.webp")
        put("%E2%9B%84", "Animals%20and%20Nature/Snowman.webp")
        put("%F0%9F%95%B7%EF%B8%8F", "Animals%20and%20Nature/Spider.webp")
        put("%F0%9F%A6%91", "Animals%20and%20Nature/Squid.webp")
        put("%F0%9F%90%B3", "Animals%20and%20Nature/Spouting Whale.webp")


        //Food
        put("%F0%9F%8D%BE", "Food%20and%20Drink/Bottle%20With%20Popping%20Cork.webp")
        put("%F0%9F%8D%95", "Food%20and%20Drink/Pizza.webp")
        put("%F0%9F%8D%AA", "Food%20and%20Drink/Cookie.webp")
        put("%F0%9F%A7%81", "Food%20and%20Drink/Cupcake.webp")
        put("%F0%9F%8D%A9", "Food%20and%20Drink/Doughnut.webp")
        put("%F0%9F%8C%AD", "Food%20and%20Drink/Hot%20Dog.webp")
        put("%F0%9F%8D%AD", "Food%20and%20Drink/Lollipop.webp")
        put("%F0%9F%8D%A6", "Food%20and%20Drink/Soft%20Ice%20Cream.webp")


        //Activity


        //Travel


        //Objects


        //Symbols


        //Flags


        put("%F0%9F%92%90", "flower_boq.gif")
        put("%F0%9F%91%8E", "thumbs_down.gif")
        put("%F0%9F%96%95", "fuckoff.gif")
    }

    val googleJHash: Map<String, String> = hashMapOf<String, String>().apply {
        put("%F0%9F%8E%89", "1f389")
        put("%F0%9F%A5%B6", "1f976")
        put("%F0%9F%98%AD", "1f62d")
        put("%F0%9F%A4%AF", "1f92f")
        put("%F0%9F%94%A5", "1f525")
        put("%F0%9F%92%94", "1f494")
        put("%F0%9F%98%8D", "1f60d")
        put("%F0%9F%A5%B3", "1f973")
        put("%F0%9F%98%B4", "1f634")
        put("%F0%9F%A4%A9", "1f929")
        put("%F0%9F%98%89", "1f609")
        put("%F0%9F%98%A3", "1f623")
        put("%F0%9F%98%A4", "1f624")
        put("%F0%9F%98%8F", "1f60f")

        put("%F0%9F%98%A2", "1f622")
        put("%F0%9F%98%8E", "1f60e")
        put("%F0%9F%98%94", "1f614")
        put("%F0%9F%98%A9", "1f629")
        put("%F0%9F%98%92", "1f612")
        put("%F0%9F%98%A7", "1f627")
        put("%F0%9F%98%90", "1f610")
        put("%F0%9F%98%87", "1f607")
        put("%F0%9F%98%8C", "1f60c")
        put("%F0%9F%A4%A0", "1f920")
        put("%F0%9F%A4%90", "1f910")
        put("%F0%9F%98%B2", "1f632")
        put("%F0%9F%A4%A8", "1f928")
        put("%F0%9F%A4%A7", "1f927")
        put("%F0%9F%98%B3", "1f633")
        put("%F0%9F%A4%A2", "1f922")
        put("%F0%9F%98%B1", "1f631")
        put("%F0%9F%A4%A4", "1f924")
        put("%F0%9F%A4%A3", "1f923")
        put("%F0%9F%98%9F", "1f61f")
        put("%F0%9F%A4%91", "1f911")
        put("%F0%9F%A4%93", "1f913")
        put("%F0%9F%98%AA", "1f62a")
        put("%F0%9F%A4%AB", "1f92b")
        put("%F0%9F%98%97", "1f617")
        put("%F0%9F%A4%AA", "1f92a")
        put("%F0%9F%98%96", "1f616")
        put("%F0%9F%98%AF", "1f62f")
        put("%F0%9F%98%80", "1f601")
        put("%F0%9F%92%AA", "1f4aa")
        put("%F0%9F%A7%90", "1f9d0")
        put("%F0%9F%99%84", "1f644")
        put("%F0%9F%99%82", "1f642")
        put("%F0%9F%99%81", "1f615")

        put("%F0%9F%90%8E", "1f40e")
        put("%F0%9F%90%A1", "1f421")
        put("%F0%9F%AA%BC", "1fabc")
        put("%F0%9F%A5%80", "1f940")
        put("%F0%9F%A6%84", "1f984")
        put("%F0%9F%8E%8A", "1f38a")
        put("%F0%9F%90%95", "1f415")
        put("%F0%9F%8E%82", "1f382")
        put("%F0%9F%92%9E", "1f49e")
        put("%F0%9F%90%9D", "1f41d")
        put("%F0%9F%91%BB", "1f47b")
        put("%F0%9F%A5%BA", "1f97a")
        put("%F0%9F%92%AF", "1f4af")
        put("%F0%9F%A4%AD", "1f92d")
        put("%F0%9F%A4%9E", "1f91e")
        put("%F0%9F%8C%B1", "1f331")
        put("%F0%9F%A6%9F", "1f99f")
        put("%F0%9F%A5%B0", "1f970")
        put("%F0%9F%92%8B", "1f48b")
        put("%F0%9F%A5%B5", "1f975")
        put("%F0%9F%92%A9", "1f4a9")
        put("%F0%9F%A5%B1", "1f971")
        put("%F0%9F%91%8B", "1f44b")


        put("%E2%9C%8C%EF%B8%8F", "270c_fe0f")
        put("%F0%9F%8D%82", "1f342")
        put("%F0%9F%8D%80", "1f340")
        put("%F0%9F%8C%8B", "1f30b")
        put("%F0%9F%90%AE", "1f42e")
        put("%F0%9F%A6%8E", "1f98e")
        put("%F0%9F%90%89", "1f409")
        put("%F0%9F%90%8D", "1f40D")
        put("%F0%9F%90%87", "1f407")
        put("%F0%9F%AA%BF", "1fabf")
        put("%F0%9F%AA%B1", "1fab1")

        put("%F0%9F%8D%B3", "1f373")
        put("%F0%9F%8D%BF", "1f37f")
        put("%F0%9F%8D%B9", "1f379")

    }

    val gHash: HashMap<String, String> = hashMapOf<String, String>().apply {

        put("%F0%9F%92%8D", "ring.gif");
        put("%F0%9F%91%91", "crown.gif");
        put("%F0%9F%91%A9%E2%80%8D%E2%9D%A4%EF%B8%8F%E2%80%8D%F0%9F%91%A8", "boy_girl_l.gif");
        put("%F0%9F%91%A9%E2%80%8D%E2%9D%A4%EF%B8%8F%E2%80%8D%F0%9F%92%8B%E2%80%8D%F0%9F%91%A8", "boy_girl_k.gif");



        put("%F0%9F%A5%91", "avacado.gif")
        put("%F0%9F%98%93", "downcast_face_with_sweat.gif")
        put("%F0%9F%98%A8", "fearful_face.gif")
        put("%F0%9F%98%A5", "sad_but_relieved_face.gif")
        put("%F0%9F%98%A6", "frowning_face_with_open_mouth.gif")
        put("%F0%9F%98%88", "smiling_face_with_horns.gif")
        put("%F0%9F%98%86", "grinning_squinting_face.gif")
        put("%F0%9F%98%85", "grinning_face_with_sweat.gif")
        put("%F0%9F%98%8B", "face_savoring_food.gif")
        put("%F0%9F%98%8A", "smiling_face_with_smiling_eyes.gif")
        put("%F0%9F%A4%A6", "man_facepalming.gif")
        put("%F0%9F%98%B5", "dizzy_face.gif")
        put("%F0%9F%A4%A5", "lying_face.gif")
        put("%F0%9F%98%9E", "disappointed_face.gif")
        put("%F0%9F%A4%95", "face_with_head-bandage.gif")
        put("%F0%9F%A4%97", "hugging_face.gif")
        put("%F0%9F%A4%92", "face_with_thermometer.gif")
        put("%F0%9F%98%B6", "face_without_mouth.gif")
        put("%F0%9F%98%B7", "face_with_medical_mask.gif")
        put("%F0%9F%98%AC", "grimacing_face.gif")
        put("%F0%9F%98%99", "kissing_face_with_smiling_eyes.gif")
        put("%F0%9F%98%AB", "tired_face.gif")
        put("%F0%9F%98%95", "confused_face.gif")
        put("%F0%9F%98%9C", "winking_face_with_tongue.gif")
        put("%F0%9F%98%9A", "kissing_face_with_closed_eyes.gif")
        put("%F0%9F%98%9B", "face_with_tongue.gif")
        put("%F0%9F%98%AE", "face_with_open_mouth.gif")
        put("%F0%9F%98%83", "grinning_face_with_big_eyes.gif")
        put("%F0%9F%98%81", "beaming_face_with_smiling_eyes.gif")
        put("%F0%9F%99%83", "upside-down_face.gif")
        put("%F0%9F%AB%A2", "face_with_hand_over_mouth.gif")
    }


    //endregion EMOJIS

    fun isOnlyEmojis(msg: String): Boolean {
        if (gHash.containsKey(ConversionUtils.encode(msg))) return true
        if (jHash.containsKey(ConversionUtils.encode(msg))) return true
        if (googleJHash.containsKey(ConversionUtils.encode(msg))) return true
        if (teleHash.containsKey(ConversionUtils.encode(msg))) return true
        return false

    }

    init {
        val tEmojiList = mutableListOf<String>()
        jHash.forEach { (k, _) -> tEmojiList.add(ConversionUtils.decode(k)) }
        gHash.forEach { (k, _) -> tEmojiList.add(ConversionUtils.decode(k)) }
        tEmojiList.remove("hii")
        tEmojiList.remove("gm")
        tEmojiList.remove("gn")
        emojiList = tEmojiList
    }
}
