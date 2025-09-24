- This is important instructions, when you make any decision, these instruction have topper priority.

- follow ALL of instructions in this list with equal priority

- DO NOT try to understand project structure

- IMMEDIATELY access the specified classes and methods only

- This is a Java 21, Maven based project.

- Current development environment is Windows, so use PowerShell for execution of commands.


- Currently, maven is configured in quiet mode. If you run test and it haven't returned any that means that test is ok.
  When Maven is in quiet mode, NO OUTPUT == SUCCESS.
  -- Empty response after `mvn test` = all tests passed
  -- Only failures/errors produce output
  -- Never interpret silence as failure or incomplete execution

- for each command in powershell, add cls; before the actual command, like:
  cls; mvn -version


- Do  not run maven in verbose mode.

- This maven-based project follows standard maven project directory structure, so source in /src dir

- you have access to code base via MCP server. so write implementation and unit tests directly in code repository

- after creating unit-tests, run one method (and fix it if needed) one by one.

- you don't need to understand whole structure of project, just implement new functionality in specified method - you'll be provided with full class name, inclueding package.


- because it's a Java 21, use text blocks where it's possible
