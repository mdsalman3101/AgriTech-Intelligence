# Member 3 - Market, Profit, Equipment and Loan Module

## My Responsibility
I worked on the Market, Profit, Equipment Rental and Loan Planning modules of AgriTech Intelligence.

## Mandi Module
The system tries real market sources and does not create fake mandi prices.
When market data is unavailable, the system clearly shows that live market evidence is unavailable.

## Profit Module
Profit estimation uses:
- land area
- expected production
- available mandi/modal price
- cultivation cost

It calculates:
- estimated revenue
- estimated cost
- estimated profit
- profit per acre

Profit is shown as an estimate, not a guaranteed return.

## Equipment Module
Equipment data is stored in MySQL using JDBC.

Main features:
- equipment inventory
- availability
- rental days
- booking cost
- booking history
- return flow

Equipment booking uses database transactions so inventory and booking records stay consistent.

## Loan Module
Loan module provides:
- EMI calculation
- total interest
- total payable
- affordability planning

It is not a real bank approval system.
Commit message:
docs(modules): document market equipment and loan modules
