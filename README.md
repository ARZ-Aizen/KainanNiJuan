# Kainan ni Juan

Kainan ni Juan is a JavaFX desktop-based point-of-sale (POS) and inventory management application designed to streamline and automate core restaurant processes. Built with a focus on a clean, modern user interface and reliable backend logic, the system replaces manual record-keeping with an efficient digital solution for handling transactions and employee data. 

## Features

* **Client-Server Architecture:** Capable of operating in a distributed environment, allowing multiple client POS terminals to connect seamlessly and synchronize with a central database server.
* **Point-of-Sale (POS) System:** Fast and efficient transaction processing for daily restaurant operations.
* **Role-Based Access Control:** Secure, customized access levels tailored for Cashiers, Admins, and Managers.
* **Employee Data Handling:** Centralized management of staff records, schedules, and administrative data.
* **Inventory Tracking:** Automated tracking of stock and ingredients to minimize shortages and waste.
* **Modern UI/UX:** Clean, intuitive interface featuring optimized layouts, dark mode support, and customized rounded containers for ease of use.

## Tech Stack & Architecture

* **Frontend/GUI:** JavaFX
* **Language:** Java
* **Database:** MariaDB
* **Database Connection Pool:** HikariCP for high-performance data access
* **Architecture:** Client-Server deployment model, structured internally using Model-View-Controller (MVC) and Data Access Object (DAO) design patterns.
* **Development Environment:** Built and optimized for IntelliJ IDEA

## Prerequisites

Before running the application, ensure you have the following installed:
* [Java Development Kit (JDK)](https://www.oracle.com/java/technologies/downloads/) (Version 11 or higher recommended)
* XAMPP
* [MariaDB](https://mariadb.org/download/) (Hosted on the central server machine)
* Your preferred IDE (IntelliJ IDEA recommended)
* Git

## Installation & Setup

### 1. Database Configuration (Server Machine)
   * Open your MariaDB terminal or client on the main server machine.
   * Import database for the application (e.g., `kainan_pos').
   * Import the provided SQL schema file to generate the required tables.
   * *Note: Ensure your MariaDB user privileges and firewall settings allow remote connections from the client machines.*

### 2. Application Setup (Client Machines)
   * Clone the repository to the client computer:
     ```bash
     git clone [https://github.com/yourusername/kainan-ni-juan.git](https://github.com/yourusername/kainan-ni-juan.git)
     cd kainan-ni-juan
     ```

### 3. Configure Database Credentials
   * Locate the database configuration file (or the class handling the HikariCP configuration) in the project structure.
     ```properties
     # Example Configuration for a Client connecting to the Server
     db.host=192.168.1.10
     db.port=3306
     db.name=kainan_pos
     db.user=posuser
     db.password=pos123
     

### 4. Build and Run
   * Open the project in IntelliJ IDEA.
   * Allow the IDE to resolve and download dependencies.
   * Run the main application class to launch Kainan ni Juan on the client terminal.
