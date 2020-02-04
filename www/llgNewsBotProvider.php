<?php
/*
llgNewsBotProvider.php
Copyright (C) 2020 Alexander Theulings, ketchupcomputing.com <alexander@theulings.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
 
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
 
You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 
Documentation for this file can be found at https://ketchupcomputing.com/llg-news-bot/
*/

$pd = json_decode(file_get_contents('php://input'));
//Make sure the required json pairs have been provided
if (!isset($pd->username) || !isset($pd->password) || !isset($pd->requestType)){
    die("ERROR 401");
}

include "../local/llgNewsBotSettings.php";

//Check the login
$conn = new mysqli($dbHost, $dbUser, $dbPass, $dbName);
if ($conn->connect_error) {
    die("ERROR 500");
}

$stmt = $conn->prepare("SELECT id, uname, pword FROM admins WHERE uname = ?");
$decodedUname = base64_decode($pd->username);
$stmt->bind_param("s", $decodedUname);
$stmt->execute();
$result = $stmt->get_result();
if ($result->num_rows == 0){
    $result->free_result();
    $conn->close();
    die("ERROR 401 - Login Invalid");
}
$decodedPword = base64_decode($pd->password);
$row = $result->fetch_assoc();
if ($decodedUname != $row["uname"] || !password_verify($decodedPword , $row["pword"])){
    $result->free_result();
    $conn->close();
    die("ERROR 401 - Login Invalid");
}
$currentUserID = $row["id"];
$result->free_result();

//Select an operation depending on the request type
switch($pd->requestType){
    //Check if there are any unreviewed URLs
    case 0:
    $stmt = $conn->prepare("SELECT id,link,title FROM articleList WHERE status = 0");
        $stmt->execute();
        $result = $stmt->get_result();
        if ($result->num_rows == 0){
            $arr = array('result' => false);    
            $result->free_result();
            $conn->close();
            die(json_encode($arr));
        }else{
            $arr = array('result' => true);    
            $result->free_result();
            $conn->close();
            die(json_encode($arr));
        }

    //Check for valid login 
    case 1:
        $conn->close();
        die("loginSuccess");

    //Request for a list of items from the database 
    case 2:
    case 3:
    case 4:
    case 5:
        if ($pd->requestType == 4 || $pd->requestType == 5){
            $stmt = $conn->prepare("SELECT id,link,title FROM articleList WHERE status = ? ORDER BY id DESC LIMIT 10");
        }else{
            $stmt = $conn->prepare("SELECT id,link,title FROM articleList WHERE status = ?");
        }
        $pd->requestType;
        $stmt->bind_param("i", $pd->requestType);
        $stmt->execute();
        $result = $stmt->get_result();

        $returnArr = array();
        while ($row = $result->fetch_assoc()){
            $item = array('id' => strval($row["id"]), 'link' => base64_encode($row["link"]), 'title' => base64_encode($row["title"]));
            $itemS = json_encode($item);
            array_push($returnArr, $itemS);
        }

        $returnString = "{\"items\":" . json_encode($returnArr) . "}";
        $result->free_result();
        $conn->close();
        die($returnString);

    //Request to update the status of an item in the database
    case 8:
    case 9:
        if (!isset($pd->id)){
            die("Request type requires ID...");
        }
        $setID = $pd->id;
        $setStatus = $pd->requestType - 5;
        $stmt = $conn->prepare("UPDATE articleList SET status = ? WHERE id = ?");
        $stmt->bind_param("ii", $setStatus, $setID);
        $stmt->execute();
        $stmt = $conn->prepare("INSERT INTO logs (user, subject, action) VALUES (?, ?, ?)");
        $stmt->bind_param("iii", $currentUserID, $setID, $setStatus);
        $stmt->execute();
        $conn->close();
        die("Executed");
        break;

    default:
        die("Unknown request type.");
}
