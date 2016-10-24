#ifndef GPSTOGGLER3_COMMANDER_H
#define GPSTOGGLER3_COMMANDER_H

#define ERR_FAILED                  1       // Command failed
#define ERR_UNKNOWN_COMMAND         2       // Command unknown

void print_string(const char *pszString);
void print_error(int error_value);

#endif //GPSTOGGLER3_COMMANDER_H
