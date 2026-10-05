# Final Project Proposal

## Course: Internet Programming B

### Submitted By

* Amer Abu Sair
* Nour Salah

---

# Project Name

**TRUE Community – Community Driven Fact Checking Platform**

---

# 1. Introduction and General Description

TRUE Community is a community-driven platform for verifying and fact-checking public and political statements.

The goal of the system is to allow the community to participate in the fact-checking process through voting, providing evidence, sharing sources, and discussing claims. Once sufficient community data is collected, an editor can review the results and publish a final verdict regarding the statement.

The system will be built using Spring Boot MVC and will implement the technologies covered throughout the course, including:

* Spring MVC
* Thymeleaf
* Spring Security
* Spring Data JPA
* MySQL
* Dependency Injection
* Sessions
* Spring Beans
* Server-Sent Events (SSE)

---

# 2. System Objectives

The platform will provide the following functionality:

* User registration and authentication
* Browsing statements awaiting verification
* Voting on statements
* Attaching evidence and source links
* Writing comments
* Community consensus calculation
* Editor review and final decision publishing
* User Trust Score calculation
* Community leaderboard and rankings

---

# 3. System Pages

## 3.1 Discovery Feed

The main landing page of the platform.

Features:

* Display statements awaiting verification
* Search functionality
* Topic filtering
* Sorting options
* Consensus preview
* Navigation to the Verification Arena

---

## 3.2 Verification Arena

The core page of the platform.

Features:

* Display full statement details
* Community voting
* Evidence submission
* Comment section
* Consensus statistics and visualizations
* Live updates using SSE

---

## 3.3 User Dashboard

Personal user area.

Features:

* Trust Score display
* Voting history
* Personal statistics
* Recent activity

---

## 3.4 Community Leaderboard

Displays top contributors within the community.

Features:

* User rankings
* Accuracy percentage
* Trust Scores
* Community statistics

---

## 3.5 Editor Desk

Administrative area accessible only to editors.

Features:

* Review community consensus
* Review submitted evidence
* Publish final decisions
* Manage the verification workflow

---

# 4. User Roles

## USER

Regular platform user.

Permissions:

* Vote on statements
* Submit evidence
* Write comments
* View statements and results

---

## EDITOR

Content moderator and verifier.

Permissions:

* Access the Editor Desk
* Review community evidence
* Publish final decisions
* Manage the verification process

---

# 5. Spring Beans and Dependency Injection

The system will use Service Beans to separate business logic from controllers and follow the MVC architecture.

Examples of planned services:

### UserService

Responsible for:

* Registration
* Authentication support
* User management
* Trust Score calculation

---

### StatementService

Responsible for:

* Statement management
* Search functionality
* Filtering
* Data retrieval for pages

---

### VoteService

Responsible for:

* Vote management
* Consensus calculation
* Vote analysis

---

### CommentService

Responsible for:

* Creating comments
* Managing comments

---

### EditorDecisionService

Responsible for:

* Editor decisions
* Statement status updates
* Trust Score updates

---

# 6. Session Usage

To fulfill the session requirement of the project, the system will store temporary user-specific information.

Initial implementation:

* Recently Viewed Statements
* Statements viewed during the current session

The information will be cleared when the session ends.

---

# 7. Spring Security

The platform will use Spring Security to provide:

* Authentication
* Authorization
* Login
* Logout
* Registration

Additional security features:

* Restricted access to editor pages
* Role management
* Encrypted password storage

---

# 8. Server-Sent Events (SSE)

The system will implement Server-Sent Events (SSE) to provide live updates.

Examples:

* New votes
* New comments
* Consensus changes
* Editor decisions

Users will receive updates in real time without refreshing the page.

---

# 9. Database Design

The system will contain the following entities:

## User

Stores user information.

Main fields:

* id
* username
* email
* password
* role
* trustScore

---

## Statement

Stores statements awaiting verification.

Main fields:

* id
* text
* speakerName
* sourceUrl
* topic
* status

---

## Vote

Stores user votes.

Main fields:

* id
* verdict
* explanation

---

## Evidence

Stores evidence supporting a vote.

Main fields:

* id
* url
* description

---

## Comment

Stores user comments.

Main fields:

* id
* content

---

## EditorDecision

Stores the final editor verdict.

Main fields:

* id
* finalVerdict
* notes

---

# 10. Entity Relationships

```text
User 1 ─── * Vote
Statement 1 ─── * Vote

Vote 1 ─── 0..1 Evidence

User 1 ─── * Comment
Statement 1 ─── * Comment

Statement 1 ─── 0..1 EditorDecision

User 1 ─── * EditorDecision
```

---

# 11. Core Business Logic

The platform will implement a Trust Score mechanism.

Concept:

* Every user starts with a base Trust Score.
* After an editor publishes a final decision, the system compares each user's vote to the final verdict.
* Users whose votes match the final decision gain Trust Score points.
* Users whose votes differ from the final decision lose Trust Score points.

This mechanism encourages accurate participation and helps build a more reliable community over time.

---

# 12. Conclusion

TRUE Community combines community participation, information analysis, and data-driven decision making into a single platform.

The project fulfills all major course requirements, including:

* Spring MVC
* Thymeleaf
* Sessions
* Beans
* Dependency Injection
* Spring Security
* JPA
* MySQL
* Multiple Entity Relationships
* Server-Side Business Logic
* Real-Time Updates

The system is designed to be modular, scalable, maintainable, and aligned with modern Spring development practices.
