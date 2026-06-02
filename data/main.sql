/*
 Navicat Premium Dump SQL

 Source Server         : trainsys
 Source Server Type    : SQLite
 Source Server Version : 3045000 (3.45.0)
 Source Schema         : main

 Target Server Type    : SQLite
 Target Server Version : 3045000 (3.45.0)
 File Encoding         : 65001

 Date: 02/06/2026 08:38:48
*/

PRAGMA foreign_keys = false;

-- ----------------------------
-- Table structure for route_section
-- ----------------------------
DROP TABLE IF EXISTS "route_section";
CREATE TABLE "route_section" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "train_id" TEXT,
  "departure_id" INTEGER,
  "arrival_id" INTEGER,
  "price" INTEGER,
  "duration" INTEGER
);

-- ----------------------------
-- Table structure for sqlite_sequence
-- ----------------------------
DROP TABLE IF EXISTS "sqlite_sequence";
CREATE TABLE "sqlite_sequence" (
  "name",
  "seq"
);

-- ----------------------------
-- Table structure for station
-- ----------------------------
DROP TABLE IF EXISTS "station";
CREATE TABLE "station" (
  "id" INTEGER,
  "name" TEXT,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for station_component
-- ----------------------------
DROP TABLE IF EXISTS "station_component";
CREATE TABLE "station_component" (
  "station_id" INTEGER,
  "component_id" INTEGER,
  PRIMARY KEY ("station_id")
);

-- ----------------------------
-- Table structure for ticket_info
-- ----------------------------
DROP TABLE IF EXISTS "ticket_info";
CREATE TABLE "ticket_info" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "train_id" TEXT,
  "departure_time" TEXT,
  "departure_station" INTEGER,
  "arrival_station" INTEGER,
  "seat_num" INTEGER,
  "price" INTEGER,
  "duration" INTEGER,
  UNIQUE ("train_id" ASC, "departure_time" ASC, "departure_station" ASC)
);

-- ----------------------------
-- Table structure for train_scheduler
-- ----------------------------
DROP TABLE IF EXISTS "train_scheduler";
CREATE TABLE "train_scheduler" (
  "train_id" TEXT,
  "seat_num" INTEGER,
  "start_time" TEXT,
  "passing_num" INTEGER,
  "stations" TEXT,
  "duration" TEXT,
  "price" TEXT,
  PRIMARY KEY ("train_id")
);

-- ----------------------------
-- Table structure for trip_info
-- ----------------------------
DROP TABLE IF EXISTS "trip_info";
CREATE TABLE "trip_info" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "user_id" INTEGER,
  "train_id" TEXT,
  "departure_station" INTEGER,
  "arrival_station" INTEGER,
  "type" INTEGER,
  "duration" INTEGER,
  "price" INTEGER,
  "departure_time" TEXT,
  "arrival_time" TEXT
);

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS "user_info";
CREATE TABLE "user_info" (
  "user_id" INTEGER,
  "username" TEXT NOT NULL,
  "password" TEXT NOT NULL,
  "privilege" INTEGER NOT NULL,
  PRIMARY KEY ("user_id")
);

-- ----------------------------
-- Auto increment value for route_section
-- ----------------------------
UPDATE "sqlite_sequence" SET seq = 93 WHERE name = 'route_section';

-- ----------------------------
-- Auto increment value for ticket_info
-- ----------------------------
UPDATE "sqlite_sequence" SET seq = 251 WHERE name = 'ticket_info';

-- ----------------------------
-- Auto increment value for trip_info
-- ----------------------------
UPDATE "sqlite_sequence" SET seq = 10 WHERE name = 'trip_info';

PRAGMA foreign_keys = true;
