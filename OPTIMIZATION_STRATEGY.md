# Optimization Strategy: Small Steps, Fast Pace

## Core Philosophy
The primary goal is to optimize the editor for **speed** and **conciseness** while ensuring **zero regressions** in functionality.

## Workflow: "Small Steps, Fast Pace, Frequent Verification"
1. **Iterative Refactoring**: Make small, focused changes. Avoid massive overhauls in a single step.
2. **Performance First**: Prioritize optimizations that reduce object allocations and improve rendering speed.
3. **Frequent Compilation**: Compile the project after every significant change (or every 2-3 minor changes) to catch errors early.
4. **Output Tracking**: Always redirect build output to a `.txt` file (e.g., `build_output.txt`) to analyze errors.
5. **Logic Preservation**: Do not change the underlying logic unless you are 100% certain it is broken or redundant.
6. **Error Recovery**: If a build fails, analyze the `build_output.txt` and revert the specific change manually. Do not rely solely on Git for tiny rollbacks to keep the momentum.
7. **Name Safety**: Respect existing variable names if they are part of the plugin interface or have subtle dependencies.

## Key Tactics
- **Compaction**: Use expression bodies and compact variable declarations to reduce code size.
- **Redundancy Removal**: Strip unnecessary qualifiers (like `this.`) and boilerplate.
- **Micro-Optimizations**: Minimize allocations in hot paths like `draw` and `layout`.
