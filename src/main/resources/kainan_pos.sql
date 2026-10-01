-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Oct 01, 2026 at 11:19 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `kainan_pos`
--

-- --------------------------------------------------------

--
-- Table structure for table `categories`
--

CREATE TABLE `categories` (
  `id` int(11) NOT NULL,
  `name` varchar(40) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `categories`
--

INSERT INTO `categories` (`id`, `name`) VALUES
(5, 'Appetizers'),
(3, 'Desserts'),
(2, 'Drinks'),
(4, 'Main Course'),
(1, 'Silog Meals');

-- --------------------------------------------------------

--
-- Table structure for table `dishes`
--

CREATE TABLE `dishes` (
  `id` bigint(20) NOT NULL,
  `category_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `description` text DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `quantity` int(11) NOT NULL DEFAULT 0,
  `available` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `dishes`
--

INSERT INTO `dishes` (`id`, `category_id`, `name`, `price`, `description`, `image_url`, `quantity`, `available`) VALUES
(4, 3, 'Banana Cue', 15.00, 'Deep-fried saba bananas coated in a crisp layer of caramelized brown sugar, traditionally served on a bamboo skewer.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/banana%20cue.png', 77, 1),
(5, 3, 'Bibingka', 50.00, 'A warm, spongy baked rice cake cooked in banana leaves, classically topped with salted egg, cheese, and grated coconut.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/bibingka.png', 200, 1),
(6, 3, 'Buko Pandan', 65.00, 'A chilled, creamy dessert combining young coconut strips, pandan-flavored gelatin cubes, and sweetened all-purpose cream.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/buko%20pandan.png', 200, 1),
(7, 3, 'Buko Pie', 250.00, 'A traditional baked pastry featuring a flaky crust filled with layers of tender, sweet young coconut meat.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/buko%20pie.png', 200, 1),
(8, 3, 'Cassava Cake', 150.00, 'A rich, moist cake made from grated cassava and coconut milk, baked with a sweet, golden custard topping.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/cassava%20cake.png', 200, 1),
(9, 3, 'Fruit Salad', 80.00, 'A festive, sweet mix of canned fruit cocktail, cream, and condensed milk, served chilled or partially frozen.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/fruit%20salad.png', 200, 1),
(10, 3, 'Ginataang Bilo Bilo', 45.00, 'A warm, sweet coconut milk stew loaded with chewy glutinous rice balls, jackfruit, tapioca pearls, and sweet potatoes.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/ginataang%20bilo%20bilo.png', 200, 1),
(11, 3, 'Halo Halo', 80.00, 'The ultimate shaved ice dessert layered with sweetened beans, jellies, fruits, leche flan, and ube, generously drizzled with evaporated milk.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/halo%20halo.png', 200, 1),
(12, 3, 'Kamote Cue', 20.00, 'Thick slices of sweet potato deep-fried with a coating of caramelized brown sugar, served on a skewer.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/kamote%20cue.png', 200, 1),
(13, 3, 'Kutsinta', 50.00, 'Chewy, sticky steamed brown rice cakes enhanced with lye water, usually served with freshly grated coconut.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/kutsinta.png', 200, 1),
(14, 3, 'Leche Flan', 120.00, 'A dense, velvety caramel custard made with egg yolks and condensed milk, topped with a clear caramel syrup.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/leche%20flan.png', 200, 1),
(15, 3, 'Mais Conyelo', 50.00, 'A refreshing glass of layered sweet corn kernels, shaved ice, sugar, and milk.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/mais%20conyelo.png', 200, 1),
(16, 3, 'Maja Blanca', 60.00, 'A smooth coconut pudding infused with sweet corn kernels and often topped with toasted coconut curds (latik) or grated cheese.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/maja%20blanca.png', 200, 1),
(17, 3, 'Puto', 60.00, 'Soft, fluffy bite-sized steamed rice cakes, frequently topped with a slice of cheese or salted egg.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/puto.png', 200, 1),
(18, 3, 'Saging Con Yelo', 50.00, 'Sweetened plantains (saba) steeped in syrup, served cold over shaved ice and milk.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/saging%20con%20yelo.png', 200, 1),
(19, 3, 'Sapin Sapin', 80.00, 'A striking, multi-layered glutinous rice and coconut dessert where each colored layer offers a slightly distinct flavor like ube and jackfruit.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/sapin%20sapin.png', 200, 1),
(20, 3, 'Turon with Ice Cream', 75.00, 'Crispy, caramelized banana spring rolls served warm alongside a generous scoop of cold ice cream.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/turon%20with%20ice%20cream.png', 200, 1),
(21, 3, 'Ube Halaya', 150.00, 'A thick, rich, and creamy jam crafted from slow-cooked purple yam, butter, and condensed milk.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/ube%20halaya.png', 200, 1),
(22, 3, 'Ube Ice Cream', 40.00, 'A uniquely Filipino ice cream flavor featuring the vibrant color and earthy, sweet taste of purple yam.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/ube%20ice%20cream.png', 200, 1),
(23, 3, 'Ube Macapuno Salad', 120.00, 'A delightful, chilled dessert blending earthy ube flavor with sweet, gelatinous macapuno (coconut sport) strings and rich cream.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Desserts/ube%20macapuno%20salad.png', 200, 1),
(24, 5, 'Calamares', 120.00, 'Crispy deep-fried squid rings, often served with a spiced vinegar or mayonnaise dip.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/calamares.png', 50, 1),
(25, 5, 'Cheese Stick', 60.00, 'Deep-fried spring rolls stuffed with cheddar cheese, served with a mayo-ketchup dip.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/cheese%20stick.png', 50, 1),
(26, 5, 'Chicharon Bulaklak', 150.00, 'Deep-fried ruffled pork fat, aggressively seasoned and served extra crispy with spiced vinegar.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/chicharon%20bulaklak.png', 50, 1),
(27, 5, 'Chicken BBQ', 120.00, 'Filipino-style grilled chicken marinated in a sweet and savory blend of soy sauce, calamansi, and banana ketchup.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/chicken%20bbq.png', 50, 1),
(28, 5, 'Chicken Skin', 90.00, 'Dangerously addictive deep-fried crispy chicken skin, best paired with spiced vinegar.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/chicken%20skin.png', 50, 1),
(29, 5, 'Dynamite Lumpia', 80.00, 'Deep-fried green chili peppers stuffed with ground pork and cheese, wrapped in a spring roll wrapper.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/dynamite%20lumpia.png', 50, 1),
(30, 5, 'Fishball', 30.00, 'Classic Filipino street food made from pulverized fish, deep-fried and served with sweet and spicy brown sauce.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/fishball.png', 50, 1),
(31, 5, 'French Fries', 60.00, 'Crispy, golden deep-fried potato strips.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/french%20fries.png', 50, 1),
(32, 5, 'Fried Shrimp Balls', 90.00, 'Crispy, savory balls made from minced shrimp and aromatics, deep-fried until golden.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/fried%20shrimp%20balls.png', 50, 1),
(33, 5, 'Fried Shrimp', 150.00, 'Deep-fried battered or breaded whole shrimps, crispy on the outside and tender inside.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/fried%20shrimp.png', 50, 1),
(34, 5, 'Kamote Fries', 50.00, 'Sweet potato strips deep-fried until crispy, a sweeter and healthier alternative to regular fries.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/kamote%20fries.png', 50, 1),
(35, 5, 'Kwek Kwek', 40.00, 'Hard-boiled quail eggs coated in a vibrant orange batter and deep-fried, served with spiced vinegar.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/kwek%20kwek.png', 50, 1),
(36, 5, 'Lumpiang Sariwa', 70.00, 'Fresh spring rolls filled with savory vegetables and meat, wrapped in a soft crepe and topped with peanut sauce.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/lumpiang%20sariwa.png', 50, 1),
(37, 5, 'Lumpiang Shanghai', 80.00, 'Bite-sized deep-fried spring rolls filled with a savory mixture of ground pork, carrots, and onions.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/lumpiang%20shanghai.png', 50, 1),
(38, 5, 'Pork BBQ', 40.00, 'Skewered pork slices marinated in a sweet-savory blend and grilled to perfection.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/pork%20bbq.png', 50, 1),
(39, 5, 'Siomai', 45.00, 'Steamed or fried pork and shrimp dumplings, traditionally served with soy sauce, calamansi, and chili garlic.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/siomai.png', 50, 1),
(40, 5, 'Tokwat Baboy', 90.00, 'A savory mix of boiled pork ears/belly and deep-fried tofu cubes, bathed in a soy sauce and vinegar dressing.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/tokwat%20baboy.png', 50, 1),
(41, 5, 'Turon', 25.00, 'Deep-fried banana (saba) and jackfruit wrapped in a spring roll wrapper, coated in caramelized brown sugar.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/turon.png', 50, 1),
(42, 5, 'Ukoy', 50.00, 'Crispy deep-fried fritters made from a batter of small shrimp, bean sprouts, and sweet potato julienne.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Appetizers/ukoy.png', 50, 1),
(43, 2, 'Avocado Shake', 80.00, 'Creamy and rich blended fresh avocado drink.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/avocado%20shake.png', 50, 1),
(44, 2, 'Banana Shake', 70.00, 'Sweet and smooth blended fresh banana drink.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/banana%20shake.png', 50, 1),
(45, 2, 'Bottled Water', 20.00, 'Purified and chilled drinking water.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/bottled%20water.png', 50, 1),
(46, 2, 'Buko Juice', 40.00, 'Fresh and refreshing young coconut water with tender coconut meat strips.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/buko%20juice.png', 50, 1),
(47, 2, 'Buko Pandan', 60.00, 'Refreshing coconut and pandan-flavored sweet beverage with jelly cubes.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/buko%20pandan.png', 50, 1),
(48, 2, 'Calamansi Juice', 50.00, 'Tangy, freshly squeezed Filipino citrus drink served over ice.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/calamansi%20juice.png', 50, 1),
(49, 2, 'Coke', 45.00, 'Classic Coca-Cola carbonated soft drink in a can.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/coke.png', 50, 1),
(50, 2, 'Cucumber Lemonade', 60.00, 'Cool and revitalizing blend of fresh cucumber and sweet lemonade.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/cucumber%20lemonade.png', 50, 1),
(51, 2, 'Four Season', 50.00, 'Sweet tropical fruit juice blend typically made of mango, pineapple, orange, and guava.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/four%20season.png', 50, 1),
(52, 2, 'Guyabano Juice', 60.00, 'Sweet and mildly tart refreshing soursop fruit juice.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/guyabano%20juice.png', 50, 1),
(53, 2, 'Hot Chocolate', 60.00, 'Warm, comforting, and rich chocolate drink.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/hot%20chocolate.png', 50, 1),
(54, 2, 'Iced Tea', 45.00, 'Sweetened cold tea beverage, classic lemon flavor.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/iced%20tea.png', 50, 1),
(55, 2, 'Mango Shake', 80.00, 'Sweet and frosty blended ripe Philippine mango drink.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/mango%20shake.png', 50, 1),
(56, 2, 'Melon Juice', 50.00, 'Refreshing chilled cantaloupe juice with shredded melon bits.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/melon%20juice.png', 50, 1),
(57, 2, 'Mountain Dew', 45.00, 'Citrus-flavored carbonated soft drink in a can.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/mountain%20dew.png', 50, 1),
(58, 2, 'Pepsi', 45.00, 'Classic Pepsi carbonated soft drink in a can.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/pepsi.png', 50, 1),
(59, 2, 'Pineapple Shake', 80.00, 'Tropical, sweet and tangy blended fresh pineapple drink.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/pineapple%20shake.png', 50, 1),
(60, 2, 'Royal', 45.00, 'Orange-flavored carbonated soft drink (Royal Tru-Orange) in a can.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/royal.png', 50, 1),
(61, 2, 'Sago at Gulaman', 45.00, 'Traditional sweet local refreshment made with tapioca pearls, gelatin, and brown sugar syrup.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/sago%20at%20gulaman.png', 50, 1),
(62, 2, 'Sprite', 45.00, 'Crisp lemon-lime flavored carbonated soft drink in a can.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/sprite.png', 50, 1),
(63, 2, 'Watermelon Shake', 75.00, 'Hydrating and perfectly sweet blended fresh watermelon drink.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Drinks/watermelon%20shake.png', 50, 1),
(64, 4, 'Afritada', 180.00, 'Savory meat stewed in tomato sauce with potatoes, carrots, and bell peppers.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/afritada.png', 50, 1),
(65, 4, 'Beef Mechado', 220.00, 'Tender beef chunks simmered slowly in a rich tomato sauce with potatoes and subtle citrus notes.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/beef%20mechado.png', 50, 1),
(66, 4, 'Beef Steak', 200.00, 'Also known as Bistek Tagalog; thin slices of beef braised in soy sauce, calamansi, and topped with onion rings.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/beef%20steak.png', 50, 1),
(67, 4, 'Bicol Express', 180.00, 'Spicy pork stew simmered in creamy coconut milk, shrimp paste, and plenty of chili peppers.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/bicol%20express.png', 50, 1),
(68, 4, 'Binagoongang Talong', 120.00, 'Fried or sautéed eggplants cooked in a rich, savory, and slightly sweet shrimp paste (bagoong).', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/binagoongang%20talong.png', 50, 1),
(69, 4, 'Caldereta', 250.00, 'Hearty and rich meat stew cooked in tomato sauce and liver spread, mixed with potatoes, carrots, and olives.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/caldereta.png', 50, 1),
(70, 4, 'Chicken Adobo', 160.00, 'The classic Filipino dish of chicken simmered in soy sauce, vinegar, garlic, bay leaves, and black peppercorns.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/chicken%20adobo.png', 50, 1),
(71, 4, 'Chicken Inasal', 170.00, 'Bacolod-style grilled chicken marinated in calamansi, pepper, coconut vinegar, and annatto.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/chicken%20inasal.png', 50, 1),
(72, 4, 'Chicken Tinola', 160.00, 'Comforting chicken soup flavored with ginger and onions, served with green papaya wedges and chili leaves.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/chicken%20tinola.png', 50, 1),
(73, 4, 'Dinuguan', 170.00, 'Rich and savory pork blood stew cooked with vinegar, garlic, and long green chili peppers.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/dinuguan.png', 50, 1),
(74, 4, 'Ginataang Manok', 170.00, 'Chicken pieces stewed slowly in creamy coconut milk with ginger and green papaya.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/ginataang%20manok.png', 50, 1),
(75, 4, 'Kare Kare', 280.00, 'Rich oxtail and tripe stew in a thick, savory peanut sauce with vegetables, always served with shrimp paste on the side.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/kare%20kare.png', 50, 1),
(76, 4, 'Lechong Kawali', 220.00, 'Deep-fried pork belly chopped into pieces, perfectly crispy on the outside and tender on the inside.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/lechong%20kawali.png', 50, 1),
(77, 4, 'Menudo', 160.00, 'Diced pork and liver stewed in tomato sauce with small cubes of potatoes, carrots, and raisins.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/menudo.png', 50, 1),
(78, 4, 'Pakbet', 140.00, 'Also known as Pinakbet; a healthy mix of indigenous vegetables like bitter melon, eggplant, and squash sautéed in shrimp paste.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/pakbet.png', 50, 1),
(79, 4, 'Pork Adobo', 170.00, 'Tender chunks of pork belly braised slowly in a savory-sour mix of soy sauce, vinegar, and crushed garlic.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/pork%20adobo%20no%20bg.png', 50, 1),
(80, 4, 'Pork Sinigang', 190.00, 'A beloved sour soup using pork cuts, simmered in a tamarind broth with kangkong, radish, and tomatoes.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/pork%20sinigang.png', 50, 1),
(81, 4, 'Sinigang na Bangus', 200.00, 'Fresh milkfish cuts in a perfectly tart tamarind-based broth with mixed native vegetables.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/sinigang%20na%20bangus.png', 50, 1),
(82, 4, 'Sinigang na Hipon', 220.00, 'Plump shrimps cooked in a signature sour tamarind soup with string beans, radish, and leafy greens.', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Main%20Course/sinigang%20na%20hipon.png', 50, 1),
(83, 1, 'Adobosilog', 110.00, 'Classic pork adobo served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/adobosilog.png', 50, 1),
(84, 1, 'Bangus Belly Silog', 130.00, 'Marinated milkfish belly served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/bangus%20belly%20silog.png', 50, 1),
(85, 1, 'Beefsilog', 120.00, 'Tender beef strips served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/beefsilog.png', 50, 1),
(86, 1, 'Chicken BBQ Silog', 120.00, 'Grilled sweet and savory chicken BBQ served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/chicken%20bbq%20silog.png', 50, 1),
(87, 1, 'Chicksilog', 110.00, 'Crispy fried chicken served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/chicksilog.png', 50, 1),
(88, 1, 'Cornsilog', 100.00, 'Sautéed corned beef served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/cornsilog.png', 50, 1),
(89, 1, 'Daing Silog', 120.00, 'Butterflied, marinated, and fried milkfish served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/daing%20silog.jpg', 50, 1),
(90, 1, 'Embutido Silog', 100.00, 'Slices of Filipino-style meatloaf served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/embutido%20silog.png', 50, 1),
(91, 1, 'Hotsilog', 90.00, 'Juicy red hotdogs served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/hotsilog.jpg', 50, 1),
(92, 1, 'Lechon Silog', 130.00, 'Crispy deep-fried pork belly chunks served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/liemposilog.jpg', 50, 1),
(93, 1, 'Liemposilog', 130.00, 'Grilled or fried pork belly served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/liemposilog.jpg', 50, 1),
(94, 1, 'Longsilog', 100.00, 'Sweet and garlicky Filipino sausages served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/hotsilog.jpg', 50, 1),
(95, 1, 'Malingsilog', 90.00, 'Fried slices of luncheon meat served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/malingsilog.png', 50, 1),
(96, 1, 'Porksilog', 110.00, 'Deep-fried pork chop served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/porksilog.png', 50, 1),
(97, 1, 'Sisigsilog', 120.00, 'Sizzling minced pork face and liver served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/sisigsilog.png', 50, 1),
(98, 1, 'Tapsilog', 110.00, 'Marinated beef tapa served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/tocilog.png', 50, 1),
(99, 1, 'Tocilog', 110.00, 'Sweet cured pork slices served with garlic fried rice and a fried egg[cite: 7].', 'file:/C:/Users/ronga/IdeaProjects/KainanNiJuan/src/main/resources/com/kainanresto/images/main/PreloadDish/Silog%20Meals/tocilog.png', 50, 1);

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('ADMIN','CASHIER','MANAGER','SUPERVISOR') NOT NULL DEFAULT 'CASHIER',
  `full_name` varchar(100) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `last_login` datetime DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `categories`
--
ALTER TABLE `categories`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `name` (`name`);

--
-- Indexes for table `dishes`
--
ALTER TABLE `dishes`
  ADD PRIMARY KEY (`id`),
  ADD KEY `category_id` (`category_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `username` (`username`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `categories`
--
ALTER TABLE `categories`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `dishes`
--
ALTER TABLE `dishes`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=101;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `dishes`
--
ALTER TABLE `dishes`
  ADD CONSTRAINT `dishes_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
