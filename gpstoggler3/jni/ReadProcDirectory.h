#ifndef GPSTOGGLER3_READPROCDIRECTORY_H
#define GPSTOGGLER3_READPROCDIRECTORY_H

#include "Command.h"

class CReadProcDirectory : public CCommand {
public:
    static const char* COMMAND;

    CReadProcDirectory  (void);
    ~CReadProcDirectory();

    virtual bool execute (std::string &output);

protected:
    bool readFile (const char *pcszFilePath, char *pszData, int nMaxData);
};

#endif //GPSTOGGLER3_READPROCDIRECTORY_H
