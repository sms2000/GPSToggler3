#ifndef GPSTOGGLER3_COMMAND_H
#define GPSTOGGLER3_COMMAND_H

#include <string>
#include <string.h>

class CCommand {
public:
    CCommand    (void);
    ~CCommand   ();

    virtual bool execute (std::string &output) = 0;
};

#endif //GPSTOGGLER3_COMMAND_H
